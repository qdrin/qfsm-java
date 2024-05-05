package org.qdrin.qfsm.machine.states;

import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangedEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent());
    Product product = context.getExtendedState().get("product", Product.class);
    ProductPrice nextPrice = context.getExtendedState().get("nextPrice", ProductPrice.class);
    nextPrice.setPeriod(1);
    List<ProductPrice> currentPrices = product.getProductPrice();
    currentPrices.removeIf(p -> p.getPriceType().equals(PriceType.RecurringCharge.name()));
    currentPrices.add(nextPrice);
    log.debug("productPrice: {}", product.getProductPrice());
    new AddActionAction(ActionSuite.CHANGE_PRICE_EXTERNAL).execute(context);  // Instant.now());
  }
}
