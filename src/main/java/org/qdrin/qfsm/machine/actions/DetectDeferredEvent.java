package org.qdrin.qfsm.machine.actions;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.StateMachineEventResult.DefaultStateMachineEventResult;
import org.springframework.statemachine.access.StateMachineAccessor;
import org.springframework.statemachine.annotation.*;
import org.springframework.statemachine.region.Region;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithStateMachine
public class DetectDeferredEvent {

  private boolean isDeferred(String event, State<String, String> state) {
    if(state.getDeferredEvents().contains(event)) return true;
    if(state.isOrthogonal()) {
      Collection<Region<String, String>> regions = ((RegionState<String, String>) state).getRegions();
      return regions.stream().filter(r -> isDeferred(event, r.getState())).count() > 0;
    }
    if(state.isComposite()) return isDeferred(event, ((RegionState<String, String>) state).getSubmachine().getState());
    return false;
  }
  // Deferred events in orthogonal states aren't detected automatically
  @OnEventNotAccepted
  public void checkDeferredEvent(StateContext<String, String> context) {
    String event = context.getEvent();
    State<String, String> source = context.getSource();
    if(isDeferred(event, source)) {
      log.warn("has deferred events!");
    }
  }
}
