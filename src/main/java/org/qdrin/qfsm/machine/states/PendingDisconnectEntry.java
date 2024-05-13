package org.qdrin.qfsm.machine.states;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.tasks.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PendingDisconnectEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState(); 
    Product product = extendedState.get("product", Product.class);
    log.debug("PendingActivateEntry started. event: {}, message: {}", context.getEvent());
    // TODO: Remove after manual mode
    List<ProductCharacteristic> chars = ExternalData.requestProductCharacteristics();
    if(chars.size() > 0) {
      product.setCharacteristic(chars);
      log.debug("product with characteristics: {}", product);
    }
    
    // ----------------------------------------------------------------------------------
    List<ProductCharacteristic> characteristics = product.getCharacteristic();
    Optional<ProductCharacteristic> ch = Optional.empty();
    if(characteristics != null) {
      ch = product.getCharacteristic().stream()
        .filter(c -> {return c.getRefName().equals("deactivation_mode");})
        .findFirst();
    }
    String productMode = ch.isPresent() ? ch.get().getValue().toString() : "Postponed";
    log.debug("deactivation mode: {}", productMode);


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

    // TODO: or characteristic-valued or event characteristic valued
    OffsetDateTime disconnectDate = product.getActiveEndDate();
    tasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.DISCONNECT)
      .wakeAt(disconnectDate)
      .build()  
    );
    paymentRes.block();
    priceRes.block();
  }
}
