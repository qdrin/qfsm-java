package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import org.qdrin.qfsm.tasks.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotPaidExit implements Action<String, String> {
  
  public void execute(StateContext<String, String> context) {
    TaskPlan tasks = context.getStateMachine().getExtendedState().get("tasks", TaskPlan.class);
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    log.debug("addToRemovePlan: SUSPEND_ENDED");
  }
}
