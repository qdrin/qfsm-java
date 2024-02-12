package org.qdrin.qfsm.machine.actions;

import java.util.Map;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.annotation.EventHeader;
import org.springframework.statemachine.annotation.EventHeaders;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.annotation.WithStateMachine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithStateMachine
public class CustomTransition {
  
  @OnTransition
  public void registerTransition(@EventHeaders Map<String, Object> headers, ExtendedState extendedState) {
    int count = (int) extendedState.getVariables().getOrDefault("transitionCount", 0) + 1;
    extendedState.getVariables().put("transitionCount", count);
  }
}
