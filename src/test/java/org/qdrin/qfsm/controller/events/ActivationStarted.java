package org.qdrin.qfsm.controller.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.qdrin.qfsm.Helper.Assertions;

import java.util.*;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.controller.ControllerStarter;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

// @SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
@Slf4j
public class ActivationStarted extends ControllerStarter {

  @Autowired
  private TestRestTemplate restTemplate;

  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    // mockServerClient.reset();
    clearDb();
  }

  @Nested
  class Simple {
    @Test
    public void activationStartedSimpleFailedNullOrderItems() throws Exception {
      String offerId = "simpleOffer1";
      String priceId = "simple1-price-trial";
      RequestEventDto event = new EventBuilder("activation_started", offerId, priceId).build();
      event.setProductOrderItems(null);
      log.debug("event: {}", event);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ErrorModel> resp = restTemplate.postForEntity(eventUrl, request, ErrorModel.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
      ErrorModel response = resp.getBody();
      assertEquals(response.getErrorCode(), "BadUserDataException");
    }

    @Test
    public void activationStartedSimpleFailedRepeatedEvent() throws Exception {
      ClassPathResource resource = new ClassPathResource("/body/request/simple/activation_started.json", getClass());
      RequestEventDto body = Helper.mapper.readValue(resource.getInputStream(), RequestEventDto.class);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(body, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEntity<ErrorModel> respBad = restTemplate.postForEntity(eventUrl, request, ErrorModel.class);
      assertEquals(HttpStatus.BAD_REQUEST, respBad.getStatusCode());
      log.debug(respBad.toString());
      ErrorModel response = respBad.getBody();
      assertEquals(response.getErrorCode(), "RepeatedEventException");
    }

    @Test
    public void activationStartedSimpleSuccess() throws Exception {
      String offerId = "simpleOffer1";
      String priceId = "simple1-price-trial";
      RequestEventDto event = new EventBuilder("activation_started", offerId, priceId).build();
      log.debug("requestEvent: {}", event);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      Assertions.assertResponseEquals(event, response);
      ProductResponseDto resultProduct = response.getProducts().get(0);

      Product product = getProduct(resultProduct.getProductId());
      TestBundle expectedBundle = new BundleBuilder(event)
        .productIds(Arrays.asList(product))
        .tarificationPeriod(0)
        .build();
      Product expectedProduct = expectedBundle.bundle;
      log.debug("expect: {}", expectedProduct);
      log.debug("actual: {}", product);
      Assertions.assertProductEquals(expectedProduct, product);
      // Testing TestBundle
      assertEquals(product.getProductId(), expectedProduct.getProductId());
    }
  }

  @Nested
  class Bundle {
    @Test
    public void activationStartedBundleSuccess() throws Exception {
      String offerId = "bundleOffer1";
      String priceId = "bundle1-price-trial";
      RequestEventDto event = new EventBuilder("activation_started", offerId, priceId)
                .componentIds("component1", "component2", "component3")
                .build();
      log.debug("requestEvent: {}", event);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      Assertions.assertResponseEquals(event, response);
      List<Product> actualProducts = getResponseProducts(response);
      TestBundle expectedBundle = new BundleBuilder(event)
          .productIds(actualProducts)
          .tarificationPeriod(0)
          .build();
      log.debug("bundle expected: {}\nbundle actual: {}", expectedBundle.bundle, actualProducts.get(0));
      Assertions.assertProductEquals(expectedBundle.bundle, actualProducts.get(0));
      log.debug("components expected: {}\n, components actual: {}",
        expectedBundle.components, actualProducts.subList(1, actualProducts.size() - 1));
      Assertions.assertProductEquals(expectedBundle.components, actualProducts.subList(1, actualProducts.size()-1));
      
    }
  }

  @Nested
  class ActivationAborted {
    @Test
    public void abortSimpleFailedDeclined() throws Exception {
      String offerId = "simpleOffer1";
      OfferDef offerDef = Helper.testOffers.getOffers().get(offerId);
      Product product = new ProductBuilder("simpleOffer1", "PENDING_ACTIVATE", "simple1-price-trial").build();
      log.debug("product: {}", product);
      JsonNode machineState = Helper.buildMachineState("PendingActivate");
      StateMachine<String, String> machine = createMachine(machineState, product);
      RequestEventDto event = new EventBuilder("activation_aborted", product).build();
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      assertEquals(event.getEvent().getRefId(), response.getRefId());
      List<ProductResponseDto> products = response.getProducts();
      assertEquals(product.getProductId(), products.get(0).getProductId());
      assertEquals("ABORTED", products.get(0).getStatus());
      assertEquals(offerId, products.get(0).getProductOfferingId());
      assertEquals(offerDef.getName(), products.get(0).getProductOfferingName());
    }
  }

  @Nested
  class Disconnect {
    @Test
    public void disconnectSimpleFailedDeclined() throws Exception {
      String offerId = "simpleOffer1";
      String priceId = "simple1-price-trial";
      RequestEventDto event = new EventBuilder("activation_started", offerId, priceId).build();
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      Product product = getProduct(response.getProducts().get(0).getProductId());
      assertNotNull(product);
      event = new EventBuilder("disconnect", product).build();
      HttpEntity<RequestEventDto> requestError = new HttpEntity<>(event, headers);
      ResponseEntity<ErrorModel> respError = restTemplate.postForEntity(eventUrl, requestError, ErrorModel.class);
      assertEquals(HttpStatus.BAD_REQUEST, respError.getStatusCode());
      ErrorModel error = respError.getBody();
      assertEquals(error.getErrorCode(), "EventDeniedException");
    }
  }
}
