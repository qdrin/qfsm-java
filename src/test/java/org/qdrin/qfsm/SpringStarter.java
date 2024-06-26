package org.qdrin.qfsm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.qdrin.qfsm.repository.*;
import org.qdrin.qfsm.service.QStateMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.statemachine.StateMachine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j

@SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
public class SpringStarter {

  @Autowired
  protected ProductRepository productRepository;

  @Autowired
  protected EventRepository eventRepository;

  @Value("${application.fsm.time.priceEndedBefore}")
  Duration priceEndedBefore;
  public Duration getPriceEndedBefore() { return priceEndedBefore; }

  @Value("${application.fsm.time.waitingPayInterval}")
  Duration waitingPayInterval;
  public Duration getWaitingPayInterval() { return waitingPayInterval; }
  

  @Value("${application.fsm.time.suspendInterval}")
  Duration suspendInterval;
  public Duration getSuspendInterval() { return suspendInterval; }

  @Autowired
  private QStateMachineService service;

  private static HttpHeaders headers = new HttpHeaders();

  public static HttpHeaders getHeaders() {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  public void clearDb() {
    productRepository.deleteAll();
    eventRepository.deleteAll();
  }

  public Product getProduct(String productId) {
    return productRepository.findById(productId).get();
  }

  public StateMachine<String, String> createMachine(TestBundle bundle) {
    Product product = bundle.drive;
    List<Product> components = bundle.components();
    StateMachine<String, String> machine = service.acquireStateMachine(product, bundle.bundle, components);
    return machine;
  }

  public void releaseMachine(String machineId) {
    service.releaseStateMachine(machineId);
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