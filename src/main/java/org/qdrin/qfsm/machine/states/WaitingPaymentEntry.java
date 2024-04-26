package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import org.qdrin.qfsm.tasks.ActionSuit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("WaitingPaymentEntry started. event: {}", context.getEvent());
    List<ActionSuit> actions = (List<ActionSuit>) context.getExtendedState().getVariables().get("actions");
    actions.add(ActionSuit.WAITING_PAY_ENDED);  // Instant.now().plusSeconds(7200)
  }
}
