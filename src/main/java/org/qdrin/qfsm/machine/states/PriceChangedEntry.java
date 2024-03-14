package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangedEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceChangedEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice nextPrice = context.getExtendedState().get("nextPrice", ProductPrice.class);
    nextPrice.setPeriod(1);
    PriceHelper.setProductPrice(context, nextPrice);
    log.debug("PriceChangedEntry productPrice: {}", PriceHelper.getProductPrice(context));
  }
}
