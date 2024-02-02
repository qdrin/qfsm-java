package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PriceActiveEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("PriceActiveEntry started. event: {}, message: {}", context.getEvent());
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    int tPeriod = (int) cvars.get("tarificationPeriod");
    log.info("PriceActiveEntry tPeriod before: {}", tPeriod);
    cvars.put("tarificationPeriod", tPeriod+1);
  }
}
