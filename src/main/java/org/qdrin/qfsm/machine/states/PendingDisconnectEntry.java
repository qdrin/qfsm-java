package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.DeleteTaskAction;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.tasks.ExternalData;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PendingDisconnectEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    log.debug("PendingActivateEntry started. event: {}, message: {}", context.getEvent());
    // TODO: Remove after manual mode
    List<ProductCharacteristic> chars = ExternalData.requestProductCharacteristics();
    if(chars.size() > 0) {
      product.setCharacteristics(chars);
      log.debug("product with characteristics: {}", product);
    }
    
    // ----------------------------------------------------------------------------------
    List<ProductCharacteristic> characteristics = product.getCharacteristics();
    Optional<ProductCharacteristic> ch = Optional.empty();
    if(characteristics != null) {
      ch = product.getCharacteristics().stream()
        .filter(c -> {return c.getRefName().equals("deactivation_mode");})
        .findFirst();
    }
    String productMode = ch.isPresent() ? ch.get().getValue().toString() : "Postponed";
    log.debug("PendingActivateEntry deactivation mode: {}", productMode);


    Mono<Message<String>> paymentOff = Mono.just(MessageBuilder
      .withPayload("payment_off").build());
    Mono<Message<String>> priceOff = Mono.just(MessageBuilder
      .withPayload("price_off").build());
    var paymentRes = context.getStateMachine().sendEvent(paymentOff).collectList();
    var priceRes = context.getStateMachine().sendEvent(priceOff).collectList();
    for(String taskname: Arrays.asList(
          "price_ended",
          "suspend_ended",
          "waiting_pay_ended",
          "change_price",
          "resume"
        )) {
      DeleteTaskAction action = new DeleteTaskAction(taskname, dataSource);
      action.execute(context);
    }
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    Consumer<TaskContext> taskFunc = ScheduledTasks::startDisconnectTask;
    // TODO: Add characteristics analysis
    TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), product.getActiveEndDate().toInstant());
    taskFunc.accept(ctx);
    paymentRes.block();
    priceRes.block();
  }
}
