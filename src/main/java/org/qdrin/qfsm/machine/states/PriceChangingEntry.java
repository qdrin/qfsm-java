package org.qdrin.qfsm.machine.states;
import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.qdrin.qfsm.machine.actions.SignalAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangingEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceChangingEntry started. event: {}, message: {}", context.getEvent(), context.getMessage());
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    // Emulate external price-calculator request;
    Product product = (Product) variables.get("product");
    int tPeriod = product.getTarificationPeriod();
    if (tPeriod == 0) {
      ProductPrice price = PriceHelper.getProductPrice(context);
      variables.put("nextPrice", price);
      SignalAction changePrice = new SignalAction("change_price");
      changePrice.execute(context);
      if(price.getProductStatus().equals("ACTIVE_TRIAL")) {
        log.debug("First trial period. Sending auto 'payment_processed'");
        SignalAction paymentProcessed = new SignalAction("payment_processed");
        paymentProcessed.execute(context);
      }
    } else {
      ProductPrice nextPrice = ExternalData.RequestProductPrice();
      variables.put("nextPrice", nextPrice);
    }
  }
}
