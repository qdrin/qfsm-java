package org.qdrin.qfsm.controller.events;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.time.ZonedDateTime;
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
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
@Slf4j
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
public class Disconnect {
  
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

  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    mockServerClient.reset();
    helper.clearDb();
  }


  @Nested
  class Simple {
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

  @Nested
  class Bundle {
    
  }
}
