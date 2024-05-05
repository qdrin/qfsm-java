package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.tasks.ActionSuit;

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
    new AddActionAction(ActionSuit.WAITING_PAY_ENDED
                        .withWakeAt(OffsetDateTime.now().plus(waitingPayInterval)))
                        .execute(context);
  }
}
