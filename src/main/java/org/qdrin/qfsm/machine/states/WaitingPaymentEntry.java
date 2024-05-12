package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.tasks.TaskDef;
import org.qdrin.qfsm.tasks.TaskSet;
import org.qdrin.qfsm.tasks.TaskType;

import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentEntry implements Action<String, String> {

  @Value("${application.fsm.time.waitingPayInterval}")
  Duration waitingPayInterval;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    TaskSet tasks = context.getStateMachine().getExtendedState().get("tasks", TaskSet.class);
    tasks.put(TaskDef.builder()
      .productId(context.getStateMachine().getId())
      .type(TaskType.WAITING_PAY_ENDED)
      .wakeAt(OffsetDateTime.now().plus(waitingPayInterval))
      .build()
    );
  }
}
