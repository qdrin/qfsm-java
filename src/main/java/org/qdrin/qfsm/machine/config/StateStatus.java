package org.qdrin.qfsm.machine.config;

import java.util.List;
import java.util.Map;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.annotation.OnStateEntry;
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
    log.debug("enter state '{}'", state.getId());
    var extendedState = context.getStateMachine().getExtendedState();
    String status = statusMap.getOrDefault(state.getId(), null);
    if(status != null) {
      log.debug("setting status to {}", status);
      Product product = (Product) extendedState.get("product", Product.class);
      product.setStatus(status);
      log.debug("productId: {}, new status: {}", product.getProductId(), product.getStatus());
      List<Product> components = (List<Product>) extendedState.getVariables().get("components");
      log.debug("components isIndepended: {}", components.stream().map(c -> c.getMachineContext().getIsIndependent()).toList());
      components.stream().filter(c -> ! c.getMachineContext().getIsIndependent()).forEach(c -> c.setStatus(status));
      log.info("[{}] status: {}", product.getProductId(), product.getStatus());
      log.debug("components: {}", components.stream().map(c -> c.getStatus()).toList());
    }
  }
}
