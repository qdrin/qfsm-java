package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.ActionSuit;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedExit implements Action<String, String> {
  @Autowired
  DataSource dataSource;
  
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendedExit started. event: {}", context.getEvent());
        List<ActionSuit> deleteActions = (List<ActionSuit>) context.getExtendedState().getVariables().get("deleteActions");
    deleteActions.add(ActionSuit.SUSPEND_ENDED);
  }
}
