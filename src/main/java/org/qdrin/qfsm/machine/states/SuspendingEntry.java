package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendingEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("SuspendingEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice price = PriceHelper.getProductPrice(context);
    price.setPeriod(0);
    log.info("SuspendingEntry productPrice: {}", price);
    PriceHelper.setProductPrice(context, price);
  }
}
