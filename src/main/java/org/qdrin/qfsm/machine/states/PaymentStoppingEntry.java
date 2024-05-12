package org.qdrin.qfsm.machine.states;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentStoppingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent());
    TaskSet tasks = context.getStateMachine().getExtendedState().get("tasks", TaskSet.class);
    tasks.put(TaskDef.builder()
      .productId(context.getStateMachine().getId())
      .type(TaskType.DISCONNECT_EXTERNAL_EXTERNAL)
      .build());
  }
}
