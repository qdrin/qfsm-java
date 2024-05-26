package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PaidEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    new SignalAction("prolong").execute(context);
    Product product = context.getStateMachine().getExtendedState().get("product", Product.class);
    int tPeriod = product.getTarificationPeriod() + 1;
    product.setTarificationPeriod(tPeriod);
    log.info("[{}] tarificationPeriod={}", product.getProductId(), tPeriod);
  }
}
