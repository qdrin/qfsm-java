package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ResumingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("ResumingEntry started. event: {}, message: {}", context.getEvent(), context.getMessage());
    ExtendedState extendedState = context.getExtendedState();
    Product product = extendedState.get("product", Product.class);
    List<ActionSuit> actions = (List<ActionSuit>) context.getExtendedState().getVariables().get("actions");
    actions.add(ActionSuit.RESUME_EXTERNAL);  // Instant.now()
  }
}
