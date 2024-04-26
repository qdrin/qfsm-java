package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import org.qdrin.qfsm.tasks.ActionSuit;
import java.util.List;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendedEntry started. event: {}", context.getEvent());
    List<ActionSuit> actions = (List<ActionSuit>) context.getExtendedState().getVariables().get("actions");
    actions.add(ActionSuit.SUSPEND_ENDED);  // Instant.now().plusSeconds(30*86400)
  }
}
