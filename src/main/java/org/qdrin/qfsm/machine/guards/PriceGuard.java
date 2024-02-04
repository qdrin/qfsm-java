package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.qdrin.qfsm.model.*;

@Slf4j
public class PriceGuard implements Guard<String, String> {
  private boolean equalDir;
  private boolean fullDir;
  private boolean equalCompare;
  private boolean fullCompare;

  public PriceGuard(Optional<Boolean> full, Optional<Boolean> equal) {
    equalCompare = false;
    fullCompare = false;
    if(! full.isEmpty()) {
      fullCompare = true;
      this.fullDir = (boolean) full.get();
    }

    if(! equal.isEmpty()) {
      this.equalCompare = true;
      this.equalDir = (boolean) equal.get();
    }
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ProductPrice price = (ProductPrice) context.getExtendedState().getVariables().get("productPrice");
    ProductPrice nextPrice = (ProductPrice) context.getExtendedState().getVariables().get("nextPrice");
    boolean res = true;
    log.info("fullCompare:{}, equalCompare:{}", fullCompare, equalCompare);
    if(fullCompare) {
      boolean isFull = nextPrice.getNextPayDate() != null;
      res = fullDir? isFull : ! isFull;
      log.info("isFull:{}, res:{}", isFull, res);
    }
    if(equalCompare) {
      boolean isEqual = price.getPriceId().equals(nextPrice.getPriceId());
      res = res && (equalDir? isEqual : ! isEqual);
    }
    log.info("res={}, price: {}, nextPrice: {}, equals? {}", res, price, nextPrice);
    return res;
  }
  
}

