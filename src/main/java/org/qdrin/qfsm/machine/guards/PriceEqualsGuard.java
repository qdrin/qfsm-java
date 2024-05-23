package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import lombok.extern.slf4j.Slf4j;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.*;

@Slf4j
public class PriceEqualsGuard implements Guard<String, String> {
  
  private boolean notEqual;

  public PriceEqualsGuard() {
    this(false);
  }

  public PriceEqualsGuard(boolean notEqual) {
    this.notEqual = notEqual;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    ProductPrice price =  product.getProductPrice(PriceType.RecurringCharge).get();
    String priceId = price == null ? "" : price.getId();
    
    ProductPrice nextPrice = extendedState.get("nextPrice", ProductPrice.class);
    boolean isResume = product.getTarificationPeriod() > 0 && price.getPeriod() == 0;
    boolean eq = isResume ? false : priceId.equals(nextPrice.getId());
    return notEqual ? ! eq : eq;
  }
  
}

