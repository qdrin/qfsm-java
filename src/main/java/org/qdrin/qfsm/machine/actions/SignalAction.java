package org.qdrin.qfsm.machine.actions;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class SignalAction implements Action<String, String> {
  
  private List<Message<String>> messages = new ArrayList<>();

  public SignalAction(List<Message<String>> messages) {
    this.messages = messages;
  }

  public SignalAction(String... signals) {
    for(String signal: signals) {
      Message<String> msg = MessageBuilder.withPayload(signal).build();
      messages.add(msg);
    }
  }

  public SignalAction(String signal) {
    messages.add(MessageBuilder.withPayload(signal).build());
  }

  @Override
  public void execute(StateContext<String, String> context) {
    Flux<Message<String>> fluxMessages = Flux.fromIterable(messages);
    var res = context.getStateMachine().sendEvents(fluxMessages);
    res.blockLast();
  }
}
