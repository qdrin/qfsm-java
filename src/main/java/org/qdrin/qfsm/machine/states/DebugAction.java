package org.qdrin.qfsm.machine.states;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DebugAction implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.warn("DebugAction {} -> {}", context.getSource().getId(), context.getTarget().getId());
  }
}
