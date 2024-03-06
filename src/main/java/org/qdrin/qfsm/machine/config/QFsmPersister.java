package org.qdrin.qfsm.machine.config;

import java.util.Map;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class QFsmPersister extends JpaPersistingStateMachineInterceptor<String, String, String> {

  @Autowired
  private ProductRepository productRepository;


  QFsmPersister(JpaStateMachineRepository jpaStateMachineRepository) {
    super(jpaStateMachineRepository);
  }
  
  @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = (Product) variables.getOrDefault("product", new Product(machineId));
    productRepository.save(product);
    variables.remove("product");
    super.write(context, machineId);
  }

  @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
      StateMachineContext<String, String> machine = super.read(machineId);
      if(machine == null) {return null;}
      Map<Object, Object> variables = machine.getExtendedState().getVariables();
      Product product = productRepository.findById(machineId).orElse(new Product());
      if(product.getProductId() == null) {
        product.setProductId(machineId);
      }
      variables.put("product", product);
      return machine;
  }
}
