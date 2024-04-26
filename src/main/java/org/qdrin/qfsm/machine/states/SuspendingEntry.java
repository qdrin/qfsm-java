package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendingEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice price = PriceHelper.getProductPrice(context);
    price.setPeriod(0);
    log.debug("SuspendingEntry productPrice: {}", price);
    PriceHelper.setProductPrice(context, price);
    List<ActionSuit> actions = (List<ActionSuit>) context.getExtendedState().getVariables().get("actions");
    actions.add(ActionSuit.SUSPEND_EXTERNAL);  // Instant.now())
  }
}
