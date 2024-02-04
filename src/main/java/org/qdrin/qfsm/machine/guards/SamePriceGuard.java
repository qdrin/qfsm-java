package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;

import org.qdrin.qfsm.model.*;

@Slf4j
public class SamePriceGuard implements Guard<String, String> {
  private boolean match;

  public SamePriceGuard(boolean match) {
    this.match = match;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ProductPrice price = (ProductPrice) context.getExtendedState().getVariables().get("productPrice");
    ProductPrice nextPrice = (ProductPrice) context.getExtendedState().getVariables().get("nextPrice");
    log.info("price: {}, nextPrice: {}, equals? {}", price, nextPrice, price.getPriceId().equals(nextPrice.getPriceId()));
    return match && price.getPriceId().equals(nextPrice.getPriceId());
  }
  
}

