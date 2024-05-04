package org.qdrin.qfsm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mockserver.client.MockServerClient;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductRelationship;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.repository.*;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

  public StateMachine<String, String> createMachine(JsonNode machineState, Product product) {
    String machineId = product.getProductId();
    if(product != null) {
      productRepository.save(product);
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
    variables.put("actions", new ArrayList<ActionSuit>());
    variables.put("deleteActions", new ArrayList<ActionSuit>());
    variables.put("product", product);
    return machine;
  }

  public StateMachine<String, String> createMachine(JsonNode machineState) {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    return createMachine(machineState, product);
  }

  public StateMachine<String, String> createMachine(Product product) {
    return createMachine(null, product);
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