package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

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
    ProductClass pclass = ProductClass.values()[product.getProductClass()];
    if(pclass.name().contains("BUNDLE_COMPONENT")) {
      product = extendedState.get("bundle", Product.class);
    }
    if(product == null) return false;
    Optional<ProductPrice> oprice = product.getProductPrice(PriceType.RecurringCharge);
    if(oprice.isEmpty()) return false;
    String prstatus = oprice.get().getProductStatus();
    // log.debug("productStatus: {}, match: {}", prstatus, match);
    return ObjectUtils.nullSafeEquals(match, prstatus);
  }
  
}

