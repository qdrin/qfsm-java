package org.qdrin.qfsm.machine.states;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.qdrin.qfsm.utils.PriceHelper;
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
    log.debug("PriceChangedEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice nextPrice = context.getExtendedState().get("nextPrice", ProductPrice.class);
    nextPrice.setPeriod(1);
    PriceHelper.setProductPrice(context, nextPrice);
    log.debug("PriceChangedEntry productPrice: {}", PriceHelper.getProductPrice(context));
    new AddActionAction(ActionSuit.CHANGE_PRICE_EXTERNAL).execute(context);  // Instant.now());
  }
}
