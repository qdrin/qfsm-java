package org.qdrin.qfsm.controller.events;

import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.controller.ControllerStarter;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import static org.qdrin.qfsm.Helper.Assertions.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivationCompleted extends ControllerStarter {

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    // mockServerClient.reset();
    clearDb();
  }

  public static Stream<Arguments> activePriceSuccess() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2", "component3")));
  }

  @ParameterizedTest
  @MethodSource
  public void activePriceSuccess(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    JsonNode machineState = buildMachineState("PendingActivate");
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds.toArray(new String[0]))
      .status("PENDING_ACTIVATE")
      .machineState(machineState)
      .tarificationPeriod(0)
      .priceNextPayDate(t1)
      .save(productRepository)
      .build();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .machineState(buildMachineState("Active", "WaitingPayment", "PriceActive"))
      .status("ACTIVE")
      .pricePeriod(1)
      .tarificationPeriod(0)
      .build();
    RequestEventDto event = new EventBuilder("activation_completed", bundle).build();
    HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
    ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
    log.debug(resp.toString());
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    ResponseEventDto response = resp.getBody();
    assertResponseEquals(event, response);
    List<Product> products = getResponseProducts(response);
    assertProductEquals(expectedBundle.products, products);
  }
}
