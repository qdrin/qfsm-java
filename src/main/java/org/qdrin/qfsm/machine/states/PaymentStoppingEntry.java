package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentStoppingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    log.debug("PaymentStoppingEntry started. event: {}, message: {}", context.getEvent());
    List<ActionSuit> actions = (List<ActionSuit>) context.getExtendedState().getVariables().get("actions");
    actions.add(ActionSuit.DISCONNECT_EXTERNAL_EXTERNAL);  // Instant.now()
  }
}
