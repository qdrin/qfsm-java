package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import java.util.List;

import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.*;

public class ProductClassGuard implements Guard<String, String> {
  private final List<ProductClass> match;

  public ProductClassGuard(List<ProductClass> match) {
    this.match = match;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    int ord = context.getStateMachine().getExtendedState().get("product", Product.class)
      .getProductClass();
    ProductClass pclass = ProductClass.values()[ord];
    return match.contains(pclass); 
  }
  
}

