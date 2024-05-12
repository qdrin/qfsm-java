package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.*;
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
    SignalAction paymentProcessed = null;
    if(tPeriod != 0) {
      TaskSet tasks = extendedState.get("tasks", TaskSet.class);
      tasks.put(TaskDef.builder()
        .productId(context.getStateMachine().getId())  
        .type(TaskType.CHANGE_PRICE)
        .build());
      return;
    } else {
      nextPrice = currentPrice;
      if(nextPrice.getProductStatus().equals("ACTIVE_TRIAL") && nextPrice.getNextPayDate() != null) {
        log.debug("First trial period. Sending auto 'payment_processed'");
        paymentProcessed = new SignalAction("payment_processed");        
      }
    }
    variables.put("nextPrice", nextPrice);
    log.debug("price: {}, nextPrice: {}", currentPrice, nextPrice);
    SignalAction changePrice = new SignalAction("change_price");
    changePrice.execute(context);
    if(paymentProcessed != null) {
      paymentProcessed.execute(context);
    }
  }
}
