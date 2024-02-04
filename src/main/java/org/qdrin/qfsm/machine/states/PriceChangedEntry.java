package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangedEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("PriceChangedEntry started. event: {}, message: {}", context.getEvent());
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    ProductPrice nextPrice = (ProductPrice) cvars.get("nextPrice");
    nextPrice.setPeriod(1);
    log.info("PriceChangedEntry productPrice: {}", nextPrice);
    cvars.put("productPrice", nextPrice);
  }
}
