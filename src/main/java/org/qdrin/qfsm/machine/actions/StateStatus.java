package org.qdrin.qfsm.machine.actions;

import java.util.Map;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.*;
import org.springframework.statemachine.annotation.OnStateEntry;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.state.State;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithStateMachine
public class StateStatus {
  private static Map<String, String> statusMap = Map.of(
    "PendingActivate", "PENDING_ACTIVATE",
    "Active", "ACTIVE",
    "ActiveTrial", "ACTIVE_TRIAL",
    "Suspended", "SUSPEND",
    "PendingDisconnect", "PENDING_DISCONNECT",
    "Disconnect", "DISCONNECT"
  );
  
  @OnStateEntry
  public void setStatus(StateMachine<String, String> stateMachine, ExtendedState extendedState) {
    State<String, String> state = stateMachine.getState();
    String status = statusMap.getOrDefault(state.getId(), null);
    log.info("setStatus id: {}, status: {}", state.getId(), status);
    if(status != null) {
      extendedState.getVariables().put("status", status);
    }
  }
}
