package org.qdrin.qfsm.machine.actions;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteActionAction implements Action<String, String> {
  private ActionSuite[] actionsToDelete;
  public DeleteActionAction(ActionSuite... actionsToDelete) {
    this.actionsToDelete = actionsToDelete;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("actionsToDelete: {}", actionsToDelete);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
    List<ActionSuite> deleteActions = (List<ActionSuite>) variables.get("deleteActions");
    for(ActionSuite a: actionsToDelete) {
        if(actions.contains(a)) {
            actions.remove(a);
        } else {
            deleteActions.add(a);
        }
    }
  }
}
