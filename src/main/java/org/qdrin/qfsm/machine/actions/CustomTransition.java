package org.qdrin.qfsm.machine.actions;

import java.util.Collection;
import java.util.function.Function;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.*;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.State;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@WithStateMachine
public class CustomTransition {

  @OnTransition
  public void registerTransition(
        // @EventHeaders Map<String, Object> headers,
        StateContext<String, String> context
        ) {
    State<String, String> source = context.getSource();
    StateMachine<String, String> machine = context.getStateMachine();
    AbstractState<String, String> mstate = (AbstractState<String, String>) machine.getState();
    
    // TODO: Check condition function for it may cause double action call
    if(source != null && mstate.isComposite()) { 
      Collection<Function<StateContext<String, String>, Mono<Void>>> exitActions = source.getExitActions();
      for(Function<StateContext<String, String>, Mono<Void>> action: exitActions) {
        log.debug("registerTransition run exitAction: {}", action.getClass().getSimpleName());
        action.apply(context).block();
      }
    }
  }
}
