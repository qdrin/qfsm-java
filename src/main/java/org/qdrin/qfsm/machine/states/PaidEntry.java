package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PaidEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    Product product = (Product) cvars.get("product");
    int tPeriod = product.getTarificationPeriod() + 1;
    log.debug("PaidEntry setting tarificationPeriod={}", tPeriod);
    product.setTarificationPeriod(tPeriod);
  }
}
