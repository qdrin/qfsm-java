package org.qdrin.qfsm.persist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.qdrin.qfsm.model.ContextEntity;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.repository.ContextRepository;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

import org.springframework.statemachine.kryo.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.qdrin.qfsm.persist.QStateMachineContextConverter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ProductStateMachinePersist implements StateMachinePersist<String, String, String> {
  Map<String, StateMachineContext<String, String>> contexts = new HashMap<>();

  final static int bufferSize = 1024*1024;
  
  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ContextRepository contextRepository;  
  
  private static QStateMachineContextConverter converter = new QStateMachineContextConverter();
  // private static StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
  // private static Kryo kryo = new Kryo();

  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = (Product) variables.getOrDefault("product", new Product());
    context.getExtendedState().getVariables().remove("product");
    ContextEntity ce = new ContextEntity();
    ce.setProductId(machineId);
    ce.setContext(converter.toBytes(context));
    if(product.getProductId() == null) {
      product.setProductId(machineId);
    }
    productRepository.save(product);
    contextRepository.save(ce);
    log.debug("saving machineId {}, product: {}", machineId, product);
    // contexts.put(machineId, context);
  }
  
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Optional<Product> op = productRepository.findById(machineId);
    Optional<ContextEntity> cep = contextRepository.findById(machineId);
    if(op.isEmpty() || cep.isEmpty()) {
      return null;
    }
    Product product = op.get();
    byte[] bcontext = cep.get().getContext();
    StateMachineContext<String, String> context = converter.toContext(bcontext);
    log.debug("read context: {}", context);
    context.getExtendedState().getVariables().put("product", product);
    if(product.getProductId() == null) {
      log.debug("new product, set productId to '{}'", machineId);
      product.setProductId(machineId);
    }
    return context;
    // return contexts.get(machineId);
  }
}

