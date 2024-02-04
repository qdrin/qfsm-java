package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.SignalAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangingEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("PriceChangingEntry started. event: {}, message: {}", context.getEvent(), context.getMessage());
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    // Emulate external price-calculator request;
    int tPeriod = (int) cvars.get("tarificationPeriod");
    if (tPeriod == 0) {
      ProductPrice price = (ProductPrice) cvars.get("productPrice");
      cvars.put("nextPrice", price);
      SignalAction act = new SignalAction("change_price");
      act.execute(context);
    } else {
      ProductPrice nextPrice = ExternalData.RequestProductPrice();
      cvars.put("nextPrice", nextPrice);
    }
  }
}
