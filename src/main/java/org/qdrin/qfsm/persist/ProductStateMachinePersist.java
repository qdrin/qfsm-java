package org.qdrin.qfsm.persist;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

import com.fasterxml.jackson.databind.JsonNode;

@Configuration
public class ProductStateMachinePersist implements StateMachinePersist<String, String, String> {
  Map<String, StateMachineContext<String, String>> contexts = new HashMap<>();

  @Autowired
  private ProductRepository productRepository;
  
  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Product product = context.getExtendedState().get("product", Product.class);
    JsonNode jcontext = QStateMachineContextConverter.toJsonNode(context);
    product.setMachineState(jcontext);
    productRepository.save(product);
  }
  
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Optional<Product> oproduct = productRepository.findById(machineId);
    if(oproduct.isEmpty()) {
      return null;
    }
    Product product = oproduct.get();
    JsonNode jcontext = product.getMachineState();
    StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(jcontext);
    context.getExtendedState().getVariables().put("product", product);
    return context;
  }
}

