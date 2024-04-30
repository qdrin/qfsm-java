package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.tasks.ActionSuit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("WaitingPaymentEntry started. event: {}", context.getEvent());
    new AddActionAction(ActionSuit.WAITING_PAY_ENDED).execute(context);
  }
}
