package org.qdrin.qfsm.controller.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.controllers.EventController;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
@Slf4j
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
public class ActivationStarted {

  @Container
  private static MockServerContainer mockServer = Helper.mockServer;

  private MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());

  @Value(value="${local.server.port}")
  private int port;

  @Value(value="${server.servlet.context-path}")
  private String basePath;

  @Value(value="${management.endpoints.web.base-path}")
  private String managePath;

  private static HttpHeaders headers = Helper.getHeaders();

  private String apiVersion = "/v1";
  private String eventUrl;

  @Autowired
  private EventController eventController;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private Helper helper;

  private HttpRequest[] getMockRequests(HttpRequest request) {
    var mockRequestsString = mockServerClient.retrieveRecordedRequests(request, Format.JSON);
    var mreq = httpRequestSerializer.deserializeArray(mockRequestsString);
    return mreq;
  }

  static ObjectMapper mapper = new ObjectMapper();
  static HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer(null);

  @BeforeAll
  static void setEnvironment() {
    headers.setContentType(MediaType.APPLICATION_JSON);
  }


  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    mockServerClient.reset();
    helper.clearDb();
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
      RequestEventDto body = mapper.readValue(resource.getInputStream(), RequestEventDto.class);
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
      Helper.Assertions.assertResponseEquals(event, response);
      ProductResponseDto resultProduct = response.getProducts().get(0);

      Product product = helper.getProduct(resultProduct.getProductId());
      TestBundle expectedBundle = new BundleBuilder(event)
        .productIds(Arrays.asList(product))
        .tarificationPeriod(0)
        .build();
      Product expectedProduct = expectedBundle.bundle;
      log.debug("expect: {}", expectedProduct);
      log.debug("actual: {}", product);
      Helper.Assertions.assertProductEquals(expectedProduct, product);
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
      Helper.Assertions.assertResponseEquals(event, response);
      List<Product> actualProducts = helper.getResponseProducts(response);
      TestBundle expectedBundle = new BundleBuilder(event)
          .productIds(actualProducts)
          .tarificationPeriod(0)
          .build();
      log.debug("expected bundle: {}", expectedBundle.bundle);
      log.debug("expected components: {}", expectedBundle.components);
      List<Product> expectedProducts = expectedBundle.products();
      Helper.Assertions.assertProductEquals(expectedProducts, actualProducts);
      
    }
  }

  @Nested
  class ActivationAborted {
    @Test
    public void abortSimpleFailedDeclined() throws Exception {
      String offerId = "simpleOffer1";
      OfferDef offerDef = helper.getTestOffers().getOffers().get(offerId);
      Product product = new ProductBuilder("simpleOffer1", "PENDING_ACTIVATE", "simple1-price-trial").build();
      log.debug("product: {}", product);
      JsonNode machineState = Helper.buildMachineState("PendingActivate");
      StateMachine<String, String> machine = helper.createMachine(machineState, product);
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
      Product product = helper.getProduct(response.getProducts().get(0).getProductId());
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
