package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.qdrin.qfsm.utils.PriceHelper;
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
    int tPeriod = ((Product) cvars.get("product")).getTarificationPeriod();
    if (tPeriod == 0) {
      ProductPrice price = PriceHelper.getProductPrice(context);
      PriceHelper.setNextPrice(context, price);
      SignalAction act = new SignalAction("change_price");
      act.execute(context);
    } else {
      ProductPrice nextPrice = ExternalData.RequestProductPrice();
      PriceHelper.setNextPrice(context, nextPrice);
    }
  }
}
