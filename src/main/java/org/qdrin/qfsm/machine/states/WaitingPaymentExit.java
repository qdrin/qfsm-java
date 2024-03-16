package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.DeleteTaskAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentExit implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("WaitingPaymentExit started. event: {}", context.getEvent());
    var action = new DeleteTaskAction("waiting_pay_ended");
    action.execute(context);
  }
}
