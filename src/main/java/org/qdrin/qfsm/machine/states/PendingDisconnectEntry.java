package org.qdrin.qfsm.machine.states;
import java.time.OffsetDateTime;
import java.util.List;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.MachineContext;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.*;
import org.qdrin.qfsm.utils.DisconnectModeCalculator;
import org.qdrin.qfsm.utils.DisconnectModeCalculator.DisconnectMode;

import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PendingDisconnectEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState(); 
    Product product = extendedState.get("product", Product.class);
    log.debug("PendingActivateEntry started. event: {}, message: {}", context.getEvent());

    DisconnectMode disconnectMode = DisconnectModeCalculator.calculate(
        product,
        (List<Characteristic>) context.getMessageHeader("characteristics"), 
        (EventProperties) context.getMessageHeader("eventProperties"));
    
    log.debug("DisconnectMode: {}", disconnectMode);
    OffsetDateTime disconnectDate = disconnectMode == DisconnectMode.POSTPONED ? product.getActiveEndDate() : OffsetDateTime.now();


    Mono<Message<String>> paymentOff = Mono.just(MessageBuilder
      .withPayload("payment_off").build());
    Mono<Message<String>> priceOff = Mono.just(MessageBuilder
      .withPayload("price_off").build());
    var paymentRes = context.getStateMachine().sendEvent(paymentOff).collectList();
    var priceRes = context.getStateMachine().sendEvent(priceOff).collectList();
    TaskPlan tasks = extendedState.get("tasks", TaskPlan.class);
    String productId = product.getProductId();
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.RESUME_EXTERNAL).build());

    product.setActiveEndDate(disconnectDate);
    tasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.DISCONNECT)
      .wakeAt(disconnectDate)
      .build()  
    );
    // Some kind of trick. Here we need to have DISCONNECT in removePlan and createPlan cause we need to delete postponed and create immediate
    tasks.getRemovePlan().add(TaskDef.builder().type(TaskType.DISCONNECT).build());
    /* Для всех зависимых ножек мы
    - добавляем таск на disconnect
    - делаем их независимыми
    - Выставляем им machineState*/
    List<Product> components = (List<Product>) extendedState.getVariables().get("components");
    JsonNode machineState = buildMachineState("PendingDisconnect", "PaymentFinal", "PriceFinal");
    for(Product component: components) {
      MachineContext machineContext = component.getMachineContext();
      if(! machineContext.getIsIndependent()) {
        component.setActiveEndDate(disconnectDate);
        machineContext.setIsIndependent(true);
        machineContext.setMachineState(machineState);
        tasks.addToCreatePlan(TaskDef.builder()
          .productId(component.getProductId())
          .type(TaskType.DISCONNECT)
          .wakeAt(disconnectDate)
          .build()
          );
      } else {
        tasks.addToCreatePlan(TaskDef.builder().productId(component.getProductId()).type(TaskType.ABORT).build());
      }
    }
    paymentRes.block();
    priceRes.block();
  }
}
