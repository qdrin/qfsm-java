package org.qdrin.qfsm.tasks;

import java.util.function.Consumer;

import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;

public enum TaskType {
  ABORT(ScheduledTasks::startAbortTask),
  PRICE_ENDED(ScheduledTasks::startPriceEndedTask),
  CHANGE_PRICE(ScheduledTasks::startChangePriceTask),
  DISCONNECT(ScheduledTasks::startDisconnectTask),
  WAITING_PAY_ENDED(ScheduledTasks::startWaitingPayEndedTask),
  SUSPEND_ENDED(ScheduledTasks::startSuspendEndedTask),
  PROLONG_EXTERNAL(ScheduledTasks::startProlongExternalTask),
  SUSPEND_EXTERNAL(ScheduledTasks::startSuspendExternalTask),
  RESUME_EXTERNAL(ScheduledTasks::startResumeExternalTask),
  CHANGE_PRICE_EXTERNAL(ScheduledTasks::startChangePriceExternalTask),
  DISCONNECT_EXTERNAL(ScheduledTasks::startDisconnectExternalTask),
  DISCONNECT_EXTERNAL_EXTERNAL(ScheduledTasks::startDisconnectExternalExternalTask);

  private Consumer<TaskContext> taskFunc;

  TaskType(Consumer<TaskContext> func) {
    this.taskFunc = func;
  }

  Consumer<TaskContext> getTaskFunc() { return taskFunc; }
}
