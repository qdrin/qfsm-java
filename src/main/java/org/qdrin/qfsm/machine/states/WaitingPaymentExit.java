package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.TaskDef;
import org.qdrin.qfsm.tasks.TaskSet;
import org.qdrin.qfsm.tasks.TaskType;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentExit implements Action<String, String> {
  @Autowired
  DataSource dataSource;
  
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskSet deleteTasks = context.getStateMachine().getExtendedState().get("deleteTasks", TaskSet.class);
    deleteTasks.put(TaskDef.builder()
      .productId(context.getStateMachine().getId())
      .type(TaskType.WAITING_PAY_ENDED)
      .build());
  }
}
