package org.qdrin.qfsm.machine.states;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PaidEntry implements Action<String, String> {
  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    int tPeriod = product.getTarificationPeriod() + 1;
    log.debug("setting tarificationPeriod={}", tPeriod);
    product.setTarificationPeriod(tPeriod);
  }
}
