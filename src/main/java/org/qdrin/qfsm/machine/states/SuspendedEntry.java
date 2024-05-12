package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.tasks.*;

import java.time.OffsetDateTime;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskSet tasks = context.getStateMachine().getExtendedState().get("tasks", TaskSet.class);
    tasks.put(TaskDef.builder()
      .productId(context.getStateMachine().getId())
      .type(TaskType.SUSPEND_ENDED)
      .wakeAt(OffsetDateTime.now().plusSeconds(30*86400))
      .build()
    );
  }
}
