package org.qdrin.qfsm.tasks;
// Действия, которыми может управлять StateMachine

import java.time.OffsetDateTime;

public enum TaskType {
  // tasks
  ABORT,
  PRICE_ENDED,
  CHANGE_PRICE,
  DISCONNECT,
  WAITING_PAY_ENDED,
  SUSPEND_ENDED,
  PROLONG_EXTERNAL,
  SUSPEND_EXTERNAL,
  RESUME_EXTERNAL,
  CHANGE_PRICE_EXTERNAL,
  DISCONNECT_EXTERNAL,
  DISCONNECT_EXTERNAL_EXTERNAL;

  private OffsetDateTime wakeAt = OffsetDateTime.now();

  public TaskType withWakeAt(OffsetDateTime wakeAt) {
    this.wakeAt = wakeAt;
    return this;
  }

  public OffsetDateTime getWakeAt() {
    return wakeAt;
  }
}
