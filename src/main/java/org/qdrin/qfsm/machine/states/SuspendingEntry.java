package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ActionSuit;
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
    Product product = context.getStateMachine().getExtendedState().get("product", Product.class);
    ProductPrice price = product.getProductPrice(PriceType.RecurringCharge).get();
    price.setPeriod(0);
    log.debug("SuspendingEntry productPrice: {}", price);
    // product.getProductPrice().removeIf(p -> p.getPriceType().equals(PriceType.RecurringCharge.name()));
    // product.getProductPrice().add(price);
    new AddActionAction(ActionSuit.SUSPEND_EXTERNAL).execute(context);  // Instant.now())
  }
}
