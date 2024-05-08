package org.qdrin.qfsm.machine.actions;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithStateMachine
public class MergeComponent implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    Product bundle = extendedState.get("bundle", Product.class);
    product.setTarificationPeriod(bundle.getTarificationPeriod());
    product.setStatus(bundle.getStatus());
    product.setTrialEndDate(bundle.getTrialEndDate());
    product.setActiveEndDate(bundle.getActiveEndDate());
    log.info("merged component {} to bundle {}", product.getProductId(), bundle.getProductId());
  }
}
