package org.qdrin.qfsm.controller.events;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class ActivationAborted {

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

  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    mockServerClient.reset();
    helper.clearDb();
  }

  @Nested
  class Simple {
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
  class Bundle {

  }
}
