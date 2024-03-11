package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.utils.PriceHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivatedGuard implements Guard<String, String> {
  private final String match;

  public ActivatedGuard(String match) {
    this.match = match;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    try {
      ProductPrice price = ((Product) context
        .getStateMachine()
        .getExtendedState()
        .getVariables()
        .get("product"))
        .getProductPrices()
        .get(0);
      String prstatus = price.getProductStatus();
      return ObjectUtils.nullSafeEquals(match, prstatus);
    } catch(Exception e) {
      log.warn("Cannot get price: {}", e.getLocalizedMessage());
      return false;
    }
    // log.debug("ActivatedGuard productStatus: {}, match: {}", prstatus, match);
  }
  
}

