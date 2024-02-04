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
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    int tPeriod = (int) cvars.get("tarificationPeriod");
    if(! context.getSource().getId().equals("PriceActive")) {
      tPeriod += 1;
      log.info("PriceActiveEntry tPeriod before: {}", tPeriod);
      cvars.put("tarificationPeriod", tPeriod);
    }
    if(tPeriod > 1) {
      new SignalAction("prolong").execute(context);
    }
  }
}
