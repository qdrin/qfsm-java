package org.qdrin.qfsm.machine.states;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.machine.actions.SignalAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangingEntry implements Action<String, String> {
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent(), context.getMessage());
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    Map<Object, Object> variables = extendedState.getVariables();
    int tPeriod = product.getTarificationPeriod();
    log.debug("tarificationPeriod: {}", tPeriod);
    ProductPrice nextPrice;
    ProductPrice currentPrice = product.getProductPrice(PriceType.RecurringCharge).get();
    List<MessageBuilder<String>> messageBuilders = new ArrayList<>();
    if(tPeriod != 0) {
      if(context.getEvent().equals("resume_price")) {
        currentPrice.setPeriod(0);
      }
      TaskPlan tasks = extendedState.get("tasks", TaskPlan.class);
      TaskDef task = TaskDef.builder().type(TaskType.CHANGE_PRICE).build();
      task.getVariables().put("eventProperties", context.getMessageHeader("eventProperties"));
      tasks.addToCreatePlan(task);
      log.debug("task created: {}, messageHeaders: {}", task, context.getMessageHeaders());
      return;
    } else {
      nextPrice = currentPrice;
      messageBuilders.add(MessageBuilder.withPayload("change_price")
        .setHeader("characteristics", Arrays.asList(
        Characteristic.builder().valueType(nextPrice.getClass().getSimpleName()).value(nextPrice).name("nextPrice").build())));
      if(nextPrice.getProductStatus().equals("ACTIVE_TRIAL") && nextPrice.getNextPayDate() != null) {
        messageBuilders.add(MessageBuilder.withPayload("payment_processed"));
      }
      new SignalAction(messageBuilders).execute(context);
    }
    log.debug("price: {}, nextPrice: {}", currentPrice, nextPrice);
  }
}
