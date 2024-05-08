package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;
import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.*;

public class ActivatedGuard implements Guard<String, String> {
  private final String match;

  public ActivatedGuard(String match) {
    this.match = match;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    if(product.getProductClass() == ProductClass.CUSTOM_BUNDLE_COMPONENT.ordinal()) {
      product = extendedState.get("bundle", Product.class);
    }
    ProductPrice price = product.getProductPrice(PriceType.RecurringCharge).get();
    if(price == null) {
      return false;
    }
    String prstatus = price.getProductStatus();
    // log.debug("productStatus: {}, match: {}", prstatus, match);
    return ObjectUtils.nullSafeEquals(match, prstatus);
  }
  
}

