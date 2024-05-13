package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotPaidEntry implements Action<String, String> {
  @Autowired
  DataSource dataSource;
  
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskPlan tasks = context.getStateMachine().getExtendedState().get("tasks", TaskPlan.class);
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());
  }
}
