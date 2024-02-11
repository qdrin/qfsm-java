package org.qdrin.qfsm.machine.states;
import java.util.Arrays;
import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PendingActivateEntry implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    var mvars = context.getExtendedState().getVariables();
    Product product = (Product) mvars.get("product");
    log.info("PendingActivateEntry start. product: {}", product);
    ProductPrice price = ExternalData.RequestProductPrice();
    product.setProductPrices(Arrays.asList(price));
    // mvars.put("product", product);
    log.info("PendingsActivateEntry exit. product: {}", product);
  }
}
