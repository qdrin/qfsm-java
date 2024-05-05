package org.qdrin.qfsm.tasks;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FsmActions {
  public void createTask(ActionSuite action) {
    log.debug("creating task: {}", action);
  };
  public void deleteTask(ActionSuite action) {
    log.debug("deleting task: {}", action);
  };
}
