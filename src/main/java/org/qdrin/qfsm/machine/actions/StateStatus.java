package org.qdrin.qfsm.machine.actions;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.annotation.OnStateEntry;
import org.springframework.statemachine.annotation.OnStateExit;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.state.State;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WithStateMachine
public class StateStatus {
  private static Map<String, String> statusMap = Map.of(
    "PendingActivate", "PENDING_ACTIVATE",
    "Aborted", "ABORTED",
    "Active", "ACTIVE",
    "ActiveTrial", "ACTIVE_TRIAL",
    "Suspended", "SUSPENDED",
    "PendingDisconnect", "PENDING_DISCONNECT",
    "Disconnect", "DISCONNECT"
  );
  
  @OnStateEntry
  public void setStatus(StateContext<String, String> context) {
    State<String, String> state = context.getTarget();
    log.debug("entry state '{}'", state.getId());
    var extendedState = context.getExtendedState();
    String status = statusMap.getOrDefault(state.getId(), null);
    if(status != null) {
      Product product = (Product) extendedState.get("product", Product.class);
      if(product != null) {
        product.setStatus(status);
      }
    }
  }
}
