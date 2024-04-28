package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.DeleteAction;
import org.qdrin.qfsm.tasks.ActionSuit;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentExit implements Action<String, String> {
  @Autowired
  DataSource dataSource;
  
  public void execute(StateContext<String, String> context) {
    log.debug("WaitingPaymentExit started. event: {}", context.getEvent());
    new DeleteAction(ActionSuit.WAITING_PAY_ENDED).execute(context);
  }
}
