package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.SignalAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceActiveEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("PriceActiveEntry started. event: {}", context.getEvent());
    new SignalAction("prolong").execute(context);
  }
}
