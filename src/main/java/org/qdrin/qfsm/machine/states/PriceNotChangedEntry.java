package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceNotChangedEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent());
    Product product = context.getStateMachine().getExtendedState().get("product", Product.class);
    ProductPrice price = product.getProductPrice(PriceType.RecurringCharge).get();
    int pPeriod = (int) price.getPeriod();
    int dur = (int) price.getDuration();
    pPeriod = dur == 0 || pPeriod < dur ? pPeriod + 1 : 1;
    price.setPeriod(pPeriod);
    // product.getProductPrice().removeIf(p -> p.getPriceType().equals(PriceType.RecurringCharge.name()));
    // product.getProductPrice().add(price);
    log.debug("productPrice: {}", price);
  }
}
