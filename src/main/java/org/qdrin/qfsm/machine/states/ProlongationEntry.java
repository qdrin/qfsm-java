package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.tasks.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ProlongationEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskPlan tasks = context.getStateMachine().getExtendedState().get("tasks", TaskPlan.class);
    tasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.PROLONG_EXTERNAL)
      .build()
    );
    log.debug("task created. taskCreatePlan: {}", tasks.getCreatePlan());
  }
}
