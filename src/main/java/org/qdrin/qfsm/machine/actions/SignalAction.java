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
  
  private List<String> signals;

  public SignalAction(List<String> signals) {
    this.signals = signals;
  }

  public SignalAction(String signal) {
    this.signals = new ArrayList<>();
    signals.add(signal);
  }

  @Override
  public void execute(StateContext<String, String> context) {
    // Flux<Message<String>> messages = Flux.empty();
    List<Message<String>> messageList = new ArrayList<>();

    for(String signal: signals) {
      log.debug("sending signal: {}", signal);
      Message<String> msg = MessageBuilder.withPayload(signal).build();
      messageList.add(msg);
      // var res = context.getStateMachine().sendEvent(msg).collectList();
      // res.block();
    }
    Flux<Message<String>> messages = Flux.fromIterable(messageList);
    var res = context.getStateMachine().sendEvents(messages);
    res.blockLast();
  }
}
