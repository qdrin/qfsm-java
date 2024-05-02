package org.qdrin.qfsm.controller;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.nullable;

import java.io.*;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.controllers.EventController;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.dto.*;
import org.qdrin.qfsm.repository.ProductRepository;
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
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
@Slf4j
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
public class CommonTest {

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

  private static String readResourceAsString(ClassPathResource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), "UTF8")) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

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
  class ContainersReady {
    @Test
    public void checkController() throws Exception {
      assertThat(eventController).isNotNull();
    }

    @Test
    public void checkMockServer() throws Exception {
      assertThat(mockServerClient.hasStarted()).isTrue();
      log.debug("mockServer. http://localhost:{}/", mockServerClient.getPort());
    }
  }

  @Nested
  class ManageMethods {
    @Test
    public void checkHealth() throws Exception {
      String url = String.format("http://localhost:%d%s%s/health", port, basePath, managePath);
      log.debug("healthcheckUrl:", url);
      var resp = restTemplate.getForEntity(url, String.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    public void checkPrometheus() throws Exception {
      ClassPathResource resource = new ClassPathResource("/body/request/activation_started.json", getClass());
      String eventPath = String.format("%s%s/event", basePath, apiVersion);
      RequestEventDto body = mapper.readValue(resource.getInputStream(), RequestEventDto.class);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> resp = restTemplate.postForEntity(eventUrl, request, String.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());

      String prometheusUrl = String.format("http://localhost:%d%s%s/prometheus", port, basePath, managePath);
      
      log.debug("prometheusUrl: {}", prometheusUrl);
      var prometheusResp = restTemplate.getForEntity(prometheusUrl, String.class);
      log.debug("prometheusResp statusCode: {}", prometheusResp.getStatusCode());
      var pbody = prometheusResp.getBody();
      assertEquals(HttpStatus.OK, prometheusResp.getStatusCode());
      assertThat(pbody.contains("http_client_requests_seconds_sum")).isTrue();
      assertThat(pbody.contains("http_server_requests_seconds_count")).isTrue();
      assertThat(pbody.contains(eventPath)).isTrue();
    }
  }
}
