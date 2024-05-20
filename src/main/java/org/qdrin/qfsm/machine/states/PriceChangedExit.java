package org.qdrin.qfsm.machine.states;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.exception.BadUserDataException;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangedExit implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    List<ProductPrice> userPrices = (List<ProductPrice>) context.getMessageHeader("userPrice");
    if(userPrices == null) {
      throw new BadUserDataException("No product price found in event");
    }
    Optional<ProductPrice> ouserPrice = userPrices.stream().filter(p -> p.getPriceType().equals(PriceType.RecurringCharge.name())).findFirst();
    if(ouserPrice.isEmpty()) {
      throw new BadUserDataException("No product price of recurringCharge type found in event");
    }
    ProductPrice userPrice = ouserPrice.get();
    userPrice.setPeriod(0);
    List<ProductPrice> currentPrices = product.getProductPrice();
    currentPrices.removeIf(p -> p.getPriceType().equals(PriceType.RecurringCharge.name()));
    currentPrices.add(userPrice);
    log.debug("productPrice: {}", product.getProductPrice());
  }
}
