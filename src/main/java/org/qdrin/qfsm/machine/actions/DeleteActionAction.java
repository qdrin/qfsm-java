package org.qdrin.qfsm.machine.actions;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteActionAction implements Action<String, String> {
  private ActionSuit[] actionsToDelete;
  public DeleteActionAction(ActionSuit... actionsToDelete) {
    this.actionsToDelete = actionsToDelete;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("actionsToDelete: {}", actionsToDelete);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    List<ActionSuit> deleteActions = (List<ActionSuit>) variables.get("deleteActions");
    for(ActionSuit a: actionsToDelete) {
        if(actions.contains(a)) {
            actions.remove(a);
        } else {
            deleteActions.add(a);
        }
    }
  }
}
