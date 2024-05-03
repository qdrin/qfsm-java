package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;
import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivatedGuard implements Guard<String, String> {
  private final String match;

  public ActivatedGuard(String match) {
    this.match = match;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ProductPrice price = context.getExtendedState().get("product", Product.class)
      .getProductPrice(PriceType.RecurringCharge)
      .get();
    String prstatus = price.getProductStatus();
    // log.debug("ActivatedGuard productStatus: {}, match: {}", prstatus, match);
    return ObjectUtils.nullSafeEquals(match, prstatus);
  }
  
}

