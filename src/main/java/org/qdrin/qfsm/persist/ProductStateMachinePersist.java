package org.qdrin.qfsm.persist;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.uml2.uml.StateMachine;
import org.qdrin.qfsm.model.ContextEntity;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ContextRepository;
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
  Map<String, StateMachineContext<String, String>> contexts = new HashMap<>();

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ContextRepository contextRepository;

  // @Autowired
  // private JpaStateMachineRepository contextRepository;

  // @Autowired
  // JpaRepositoryStateMachinePersist<String, String> contextPersist;

  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = (Product) variables.getOrDefault("product", new Product());
    if(product.getProductId() == null) {
      product.setProductId(machineId);
    }
    productRepository.save(product);
    log.debug("saving machineId {}, product: {}", machineId, product);
    ContextEntity ce = new ContextEntity();
    ce.setMachineId(machineId);
    ce.setContext(context.getState());
    log.debug("saving machineId {}, contextEntity: {}", machineId, ce);
    contexts.put(machineId, context);
  }
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Product product = productRepository.findById(machineId).orElse(new Product());
    ContextEntity ce = contextRepository.findById(machineId).orElse(new ContextEntity());
    log.debug("read contextEntity: {}", ce);
    if(product.getProductId() == null) {
      log.debug("new product, set productId to '{}'", machineId);
      product.setProductId(machineId);
    }
    log.debug("read product: {}", product);
    return contexts.get(machineId);
  }
}

