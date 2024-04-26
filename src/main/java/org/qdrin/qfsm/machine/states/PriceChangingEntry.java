package org.qdrin.qfsm.machine.states;
import java.time.Instant;
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
    Product product = context.getExtendedState().get("product", Product.class);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    int tPeriod = product.getTarificationPeriod();
    ProductPrice nextPrice;
    SignalAction paymentProcessed = null;
    if(tPeriod != 0) {
      nextPrice = ExternalData.requestProductPrice();
    } else {
      nextPrice = PriceHelper.getProductPrice(context);
      if(nextPrice.getProductStatus().equals("ACTIVE_TRIAL")) {
        log.debug("First trial period. Sending auto 'payment_processed'");
        paymentProcessed = new SignalAction("payment_processed");        
      }
    }
    variables.put("nextPrice", nextPrice);
    SignalAction changePrice = new SignalAction("change_price");
    changePrice.execute(context);
    if(paymentProcessed != null) {
      paymentProcessed.execute(context);
    }
  }
}
