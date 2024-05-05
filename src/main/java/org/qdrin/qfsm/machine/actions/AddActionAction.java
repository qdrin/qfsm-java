package org.qdrin.qfsm.machine.actions;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddActionAction implements Action<String, String> {
  private ActionSuit[] actionsToAdd;
  public AddActionAction(ActionSuit... actionsToAdd) {
    this.actionsToAdd = actionsToAdd;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("actionsToDelete: {}", actionsToAdd);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    for(ActionSuit a: actionsToAdd) {
        if(actions.contains(a)) {
            actions.remove(a);
        }
        actions.add(a);
    }
  }
}
