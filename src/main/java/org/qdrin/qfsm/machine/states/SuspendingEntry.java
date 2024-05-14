package org.qdrin.qfsm.machine.states;

import java.time.OffsetDateTime;

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
public class SuspendingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent());
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    ProductPrice price = product.getProductPrice(PriceType.RecurringCharge).get();
    product.setActiveEndDate(OffsetDateTime.now());
    price.setPeriod(0);
    log.debug("productPrice: {}", price);
    TaskPlan tasks = extendedState.get("tasks", TaskPlan.class);
    tasks.addToCreatePlan(TaskDef.builder().type(TaskType.SUSPEND_EXTERNAL).build());
  }
}
