package org.qdrin.qfsm.machine.states;
import java.time.OffsetDateTime;
import java.util.List;

import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductRelationship;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class PendingActivateEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    log.debug("event: {}, message: {}", context.getEvent());
    product.setTarificationPeriod(0);
    OffsetDateTime t0 = OffsetDateTime.now();
    product.setProductStartDate(t0);
    product.getMachineContext().setIsIndependent(true);
    List<Product> components = (List<Product>) extendedState.getVariables().get("components");
    components.stream().forEach((c) -> {
      c.setTarificationPeriod(0);
      c.setProductStartDate(t0);
    });
    log.debug("set tarificationPeriod: 0, productStartDate: {}", t0);
    ProductClass pclass = ProductClass.values()[product.getProductClass()];
    if(pclass == ProductClass.CUSTOM_BUNDLE_COMPONENT) {
      Product bundle = extendedState.get("bundle", Product.class);
      ProductRelationship relation = new ProductRelationship(product);
      bundle.getProductRelationship().add(relation);
      log.info("set relation {} from bundle {} to component", relation, bundle.getProductId());
    }
  }
}
