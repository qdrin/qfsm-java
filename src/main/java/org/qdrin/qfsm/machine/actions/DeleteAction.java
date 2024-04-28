package org.qdrin.qfsm.machine.actions;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteAction implements Action<String, String> {
  private ActionSuit[] actionsToDelete;
  public DeleteAction(ActionSuit... actionsToDelete) {
    this.actionsToDelete = actionsToDelete;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("DeleteAction.execute actionsToDelete: {}", actionsToDelete);
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
