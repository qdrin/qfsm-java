package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.DeleteAction;
import org.qdrin.qfsm.tasks.ActionSuit;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotPaidEntry implements Action<String, String> {
  @Autowired
  DataSource dataSource;
  
  public void execute(StateContext<String, String> context) {
    log.debug("NotPaidEntry started. event: {}", context.getEvent());
    new DeleteAction(ActionSuit.PRICE_ENDED).execute(context);
  }
}
