package org.qdrin.qfsm.persist;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.qdrin.qfsm.model.ContextEntity;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ContextRepository;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachine;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

import org.springframework.statemachine.kryo.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ProductStateMachinePersist implements StateMachinePersist<String, String, String> {
  Map<String, StateMachineContext<String, String>> contexts = new HashMap<>();

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ContextRepository contextRepository;

  @Autowired
  private JpaStateMachineRepository jpaContextRepository;

  private static StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
  private static Kryo kryo = new Kryo();

  // @Autowired
  // JpaRepositoryStateMachinePersist<String, String> contextPersist;

  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = (Product) variables.getOrDefault("product", new Product());
    JpaRepositoryStateMachine jpaRec = new JpaRepositoryStateMachine();
    Output output = new Output(2048);
    serializer.write(kryo, output, context);
    ContextEntity ce = new ContextEntity();
    ce.setMachineId(machineId);
    ce.setContext(output.toBytes());
    output.close();
    contextRepository.save(ce);

    if(product.getProductId() == null) {
      product.setProductId(machineId);
    }
    productRepository.save(product);
    log.debug("saving machineId {}, product: {}", machineId, product);
    log.debug("saving machineId {}, contextEntity: {}", machineId, ce);
    contexts.put(machineId, context);
  }
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Product product = productRepository.findById(machineId).orElse(new Product());
    ContextEntity ce = contextRepository.findById(machineId).orElse(new ContextEntity());
    Input input = new Input(ce.getContext());
    StateMachineContext<String, String> ctx = serializer.read(kryo, input, );
    StateMachineContext<String, String> context;
    log.debug("read contextEntity: {}", ce);
    if(product.getProductId() == null) {
      log.debug("new product, set productId to '{}'", machineId);
      product.setProductId(machineId);
    }
    log.debug("read product: {}", product);
    return contexts.get(machineId);
  }
}

