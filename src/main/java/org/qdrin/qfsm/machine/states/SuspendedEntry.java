package org.qdrin.qfsm.machine.states;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.tasks.*;

import java.time.OffsetDateTime;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskPlan tasks = context.getStateMachine().getExtendedState().get("tasks", TaskPlan.class);
    tasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.SUSPEND_ENDED)
      .wakeAt(OffsetDateTime.now().plusSeconds(30*86400))
      .build()
    );
  }
}
