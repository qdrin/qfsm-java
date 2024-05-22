package org.qdrin.qfsm.machine.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class SignalAction implements Action<String, String> {
  
  private List<MessageBuilder<String>> builders = new ArrayList<>();

  public SignalAction(List<MessageBuilder<String>> builders) {
    this.builders = builders;
  }

  public SignalAction(String... signals) {
    for(String signal: signals) {
      MessageBuilder<String> builder = MessageBuilder.withPayload(signal);
      builders.add(builder);
    }
  }

  public SignalAction(String signal) {
    builders.add(MessageBuilder.withPayload(signal));
  }

  @Override
  public void execute(StateContext<String, String> context) {
    List<Message<String>> messages = new ArrayList();
    builders.stream().forEach(b -> b.copyHeadersIfAbsent(context.getMessageHeaders()));
    builders.forEach(b -> messages.add(b.build()));
    Flux<Message<String>> fluxMessages = Flux.fromIterable(messages);
    var res = context.getStateMachine().sendEvents(fluxMessages);
    res.blockLast();
  }
}
