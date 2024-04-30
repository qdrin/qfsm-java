package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.tasks.ActionSuit;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendedEntry started. event: {}", context.getEvent());
    new AddActionAction(ActionSuit.SUSPEND_ENDED).execute(context);  // Instant.now().plusSeconds(30*86400)
  }
}
