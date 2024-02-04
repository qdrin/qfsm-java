package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendingEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("SuspendingEntry started. event: {}, message: {}", context.getEvent());
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    ProductPrice price = (ProductPrice) cvars.get("productPrice");
    price.setPeriod(0);
    log.info("SuspendingEntry productPrice: {}", price);
    cvars.put("productPrice", price);
  }
}
