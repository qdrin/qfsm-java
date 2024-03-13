package org.qdrin.qfsm.machine.states;
import java.util.Arrays;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class EntryExit implements Action<String, String> {
  @Override
  public void execute(StateContext<String, String> context) {
    Product product = new Product();
    product.setProductId(context.getStateMachine().getId());
    ProductPrice price = ExternalData.RequestProductPrice();
    // product.setProductPrices(Arrays.asList(price));
    product.setProductPrices(price);
    context.getStateMachine().getExtendedState().getVariables().put("product", product);
    log.debug("EntryExit product: {}", product);
  }
}
