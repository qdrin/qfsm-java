package org.qdrin.qfsm.machine.states;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ResumingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent(), context.getMessage());
    TaskPlan tasks = context.getStateMachine().getExtendedState().get("tasks", TaskPlan.class);
    tasks.addToCreatePlan(TaskDef.builder().type(TaskType.RESUME_EXTERNAL).build());
  }
}
