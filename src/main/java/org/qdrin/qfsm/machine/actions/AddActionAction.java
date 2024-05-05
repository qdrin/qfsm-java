package org.qdrin.qfsm.machine.actions;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddActionAction implements Action<String, String> {
  private ActionSuite[] actionsToAdd;
  public AddActionAction(ActionSuite... actionsToAdd) {
    this.actionsToAdd = actionsToAdd;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("actionsToDelete: {}", actionsToAdd);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
    for(ActionSuite a: actionsToAdd) {
        if(actions.contains(a)) {
            actions.remove(a);
        }
        actions.add(a);
    }
  }
}
