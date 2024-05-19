package org.qdrin.qfsm.machine.config;

import java.util.List;
import java.util.Map;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.OnStateEntry;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.state.State;

import com.fasterxml.jackson.databind.JsonNode;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.service.QStateMachineContextConverter;

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
    log.debug("enter state '{}'", state.getId());
    StateMachine<String, String> machine = context.getStateMachine();
    ExtendedState extendedState = machine.getExtendedState();
    Product product = extendedState.get("product", Product.class);
    List<Product> components = extendedState.get("components", List.class);
    QStateMachineContextConverter.recalcMachineStates(context);
    String status = statusMap.getOrDefault(state.getId(), null);
    if(status != null) {
      log.debug("setting status to {}", status);
      product.setStatus(status);
      components.stream().filter(c -> ! c.getMachineContext().getIsIndependent()).forEach(c -> c.setStatus(status));
      log.info("[{}] status: {}", product.getProductId(), product.getStatus());
      log.debug("components: {}", components.stream().map(c -> c.getStatus()).toList());
    }
  }
}
