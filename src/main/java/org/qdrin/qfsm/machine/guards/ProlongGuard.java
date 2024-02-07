package org.qdrin.qfsm.machine.guards;

import org.qdrin.qfsm.model.Product;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProlongGuard implements Guard<String, String> {

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    boolean res = new ActiveStatesGuard(Arrays.asList("Paid", "PriceActive")).evaluate(context);
    Product product = (Product) context.getExtendedState().getVariables().get("product");
    log.info("ProlongGuard product: {}", product);
    int tPeriod = product.getTarificationPeriod();
    return res && (tPeriod != 1);
  }
}

