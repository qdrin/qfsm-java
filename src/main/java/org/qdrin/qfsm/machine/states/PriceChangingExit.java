package org.qdrin.qfsm.machine.states;
import java.util.Arrays;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangingExit implements Action<String, String> {
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}, message: {}", context.getEvent(), context.getMessage());
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    // При выходе из Suspended мы обнуляем прайс, а здесь выставляем его равным nextPrice
    if(product.getProductPrice() == null || product.getProductPrice().isEmpty()) {
      ProductPrice nextPrice = extendedState.get("nextPrice", ProductPrice.class);
      product.setProductPrice(Arrays.asList(nextPrice));
    }
  }
}
