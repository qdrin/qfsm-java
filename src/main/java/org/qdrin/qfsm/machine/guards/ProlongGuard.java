package org.qdrin.qfsm.machine.guards;

import org.qdrin.qfsm.model.Product;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import java.util.Arrays;

public class ProlongGuard implements Guard<String, String> {

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    boolean res = new ActiveStatesGuard(Arrays.asList("Paid", "PriceActive")).evaluate(context);
    Product product = context.getStateMachine().getExtendedState().get("product", Product.class);
    int tPeriod = product.getTarificationPeriod();
    // log.debug("res: {}, tarificationPeriod: {}", res, tPeriod);
    return res && (tPeriod > 1);
  }
}

