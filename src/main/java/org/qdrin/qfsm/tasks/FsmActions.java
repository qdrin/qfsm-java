package org.qdrin.qfsm.tasks;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FsmActions {
  public void createTask(TaskDef task) {
    log.debug("creating task: {}", task.getType());
  };
  public void deleteTask(TaskDef task) {
    log.debug("deleting task: {}", task.getType());
  };
}
