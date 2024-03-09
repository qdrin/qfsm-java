package org.qdrin.qfsm.persist;

import java.util.HashMap;
import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachine;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ProductStateMachinePersist implements StateMachinePersist<String, String, String> { 

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private JpaStateMachineRepository contextRepository;

  @Autowired
  JpaRepositoryStateMachinePersist<String, String> contextPersist;

  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = (Product) variables.getOrDefault("product", new Product());
    if(product.getProductId() == null) {
      product.setProductId(machineId);
    }
    productRepository.save(product);
    log.debug("saving machineId {}, context[{}]: {}", machineId, context.getId(), context);
    contextPersist.write(context, machineId);
  }
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Product product = productRepository.findById(machineId).orElse(new Product());
    StateMachineContext<String, String> context = contextPersist.read(machineId);
    if(product.getProductId() == null) {
      log.debug("new product, set productId to '{}'", machineId);
      product.setProductId(machineId);
    }
    log.debug("read product: {}", product);
    if(context != null) {
      context.getExtendedState().getVariables().put("product", product);
    }
    return context;
  }
}

