package org.qdrin.qfsm.tasks;
// Действия, которыми может управлять StateMachine

import java.time.OffsetDateTime;

public enum ActionSuit {
  // tasks
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
  DISCONNECT_EXTERNAL_EXTERNAL,

  // bundle convertions
  CREATE_CONTEXT,   // создать контекст у "ноги" и ждать по ней событий
  COPY_CONTEXT,     // Скопировать контекст с "головы" бандла (в части Usage, Payment и Price - ???)
  REMOVE_CONTEXT;   // удалить контекст у "ноги"

  private OffsetDateTime wakeAt = OffsetDateTime.now();

  public ActionSuit withWakeAt(OffsetDateTime wakeAt) {
    this.wakeAt = wakeAt;
    return this;
  }

  public OffsetDateTime getWakeAt() {
    return wakeAt;
  }
}
