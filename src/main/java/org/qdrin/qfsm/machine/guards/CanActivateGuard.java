package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import com.fasterxml.jackson.databind.JsonNode;

import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.*;

public class CanActivateGuard implements Guard<String, String> {
  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
      Product product = extendedState.get("product", Product.class);
      ProductClass pclass = ProductClass.values()[product.getProductClass()];
      switch (pclass) {
        case SIMPLE:
        case BUNDLE:
        case CUSTOM_BUNDLE:
          return true;
        case CUSTOM_BUNDLE_COMPONENT:
          Product bundle = extendedState.get("bundle", Product.class);
          JsonNode machineState = bundle.getMachineState();
          return machineState.toString().contains("Activated");
        default:
          return false;
      }
  }
  
}

