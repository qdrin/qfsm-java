package org.qdrin.qfsm.machine.states;

import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.ExtendedState;
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
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    ProductPrice nextPrice = extendedState.get("nextPrice", ProductPrice.class);
    nextPrice.setPeriod(1);
    List<ProductPrice> currentPrices = product.getProductPrice();
    currentPrices.removeIf(p -> p.getPriceType().equals(PriceType.RecurringCharge.name()));
    currentPrices.add(nextPrice);
    log.debug("productPrice: {}", product.getProductPrice());
    TaskSet tasks = extendedState.get("tasks", TaskSet.class);
    tasks.put(TaskDef.builder()
      .productId(context.getStateMachine().getId())
      .type(TaskType.CHANGE_PRICE_EXTERNAL)
      .build());
  }
}
