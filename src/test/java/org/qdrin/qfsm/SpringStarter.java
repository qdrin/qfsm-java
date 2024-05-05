package org.qdrin.qfsm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.repository.*;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.service.StateMachineService;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
public class SpringStarter {
  
  @Autowired
  ProductRepository productRepository;

  @Autowired
  EventRepository eventRepository;

  @Autowired
  ContextRepository contextRepository;

  @Resource(name = "stateMachinePersist")
  private ProductStateMachinePersist persist;

  @Autowired
  StateMachineService<String, String> service;

  @Value("${application.fsm.time.priceEndedBefore}")
  Duration priceEndedBefore;
  public Duration getPriceEndedBefore() { return priceEndedBefore; }

  @Value("${application.fsm.time.waitingPayInterval}")
  Duration waitingPayInterval;
  public Duration getWaitingPayInterval() { return waitingPayInterval; }

  private static HttpHeaders headers = new HttpHeaders();

  public static HttpHeaders getHeaders() {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  public void clearDb() {
    productRepository.deleteAll();
    contextRepository.deleteAll();
    eventRepository.deleteAll();
  }

  public Product getProduct(String productId) {
    return productRepository.findById(productId).get();
  }

  public StateMachine<String, String> createMachine(TestBundle bundle) {
    String machineId = bundle.bundle.getProductId();
    Product product = bundle.bundle;
    List<Product> components = bundle.components == null ? new ArrayList<>() : bundle.components;
    if(product.getMachineState() == null) {
      product.setMachineState(Helper.buildMachineState("PendingActivate"));
    }
    JsonNode machineState = product.getMachineState();
    JsonNode componentMachineState = Helper.buildComponentMachineState(machineState);
    productRepository.save(product);
    for(Product component: components) {
      component.setMachineState(componentMachineState);
      productRepository.save(component);
    }
    if(machineState != null) {
      try {
        // StateMachineContext<String, String> context = createContext(machineState);
        StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
        persist.write(context, machineId);
      } catch(Exception e) {
        log.error("cannot write context to DB: {}", e.getLocalizedMessage());
      }
    }
    StateMachine<String, String> machine = service.acquireStateMachine(machineId);
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    variables.put("actions", new ArrayList<ActionSuite>());
    variables.put("deleteActions", new ArrayList<ActionSuite>());
    variables.put("product", product);
    variables.put("components", components);
    // variables.put("bundle", null);
    return machine;
  }

  public StateMachine<String, String> createMachine(JsonNode machineState) {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    product.setMachineState(machineState);
    TestBundle bundle = new BundleBuilder(Arrays.asList(product)).build();
    return createMachine(bundle);
  }

  public StateMachine<String, String> createMachine(Product product) {
    TestBundle bundle = new BundleBuilder(Arrays.asList(product)).build();
    return createMachine(bundle);
  }
  public StateMachine<String, String> createMachine() {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    return createMachine(product);
  }

  public List<Product> getResponseProducts(ResponseEventDto response) {
    List<Product> result = new ArrayList<>();
    for(ProductResponseDto rproduct: response.getProducts()) {
      Product product = productRepository.findById(rproduct.getProductId()).get();
      result.add(product);
    }
    return result;
  }
}