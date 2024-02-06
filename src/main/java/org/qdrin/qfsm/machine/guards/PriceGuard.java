package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.utils.PriceHelper;

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
    ProductPrice price =  PriceHelper.getProductPrice(context);
    ProductPrice nextPrice = PriceHelper.getNextPrice(context);
    boolean res = true;
    if(fullCompare) {
      boolean isFull = nextPrice.getNextPayDate() != null;
      res = fullDir? isFull : ! isFull;
    }
    if(equalCompare) {
      boolean isEqual = price.getPriceId().equals(nextPrice.getPriceId());
      res = res && (equalDir? isEqual : ! isEqual);
    }
    return res;
  }
  
}

