package org.qdrin.qfsm.machine.actions;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class SignalAction implements Action<String, String> {
  private String signal;
  public SignalAction(String signal) {
    this.signal = signal;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    Mono<Message<String>> msg = Mono.just(MessageBuilder
      .withPayload(signal).build());
    var res = context.getStateMachine().sendEvent(msg).collectList();
    res.block();
  }
}
