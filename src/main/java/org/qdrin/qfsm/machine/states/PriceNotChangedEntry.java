package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceNotChangedEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.info("PriceNotChangedEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice price = PriceHelper.getProductPrice(context);
    int pPeriod = (int) price.getPeriod();
    int dur = (int) price.getDuration();
    pPeriod = dur == 0 || pPeriod < dur ? pPeriod + 1 : 1;
    price.setPeriod(pPeriod);
    PriceHelper.setProductPrice(context, price);
    log.info("PriceNotChangedEntry productPrice: {}", price);
  }
}
