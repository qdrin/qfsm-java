package org.qdrin.qfsm.web;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.HashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.controllers.EventController;
import org.qdrin.qfsm.exception.RepeatedEventException;
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
import org.springframework.util.FileCopyUtils;
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
public class EventControllerTest {
  public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
  @Container
  public static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

  public MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());

  @Value(value="${local.server.port}")
  private int port;

  @Value(value="${server.servlet.context-path}")
  private String basePath;

  @Value(value="${management.endpoints.web.base-path}")
  private String managePath;

  private static HttpHeaders headers = new HttpHeaders();

  private String apiVersion = "/v1";
  private String eventUrl;

  @Autowired
  private EventController eventController;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private Helper helper;

  private static Helper.TestEvent.Builder eventBuilder = new Helper.TestEvent.Builder();

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


  public static void dbInitFunction(Connection connection) throws Exception {
    ClassPathResource resource = new ClassPathResource("/db/init_db.sql", EventControllerTest.class);
    String sql = readResourceAsString(resource)
                .replace("{mockPort}", String.valueOf(mockServer.getServerPort()))
                .replace("{mockHost}", String.valueOf(mockServer.getHost()));
    connection.createStatement().execute(sql);
  }

  public void joinProcess(String processId) throws InterruptedException {
    joinProcess(processId, 10);
  }
  public void joinProcess(String processId, int secondsToWait) throws InterruptedException {
    String url = String.format("http://localhost:%d%s/engine-rest/history/process-instance/%s", port, basePath, processId);
    log.debug("joinProcess url: {}", url);
    boolean finished = false;
    var tend = ZonedDateTime.now().plusSeconds(secondsToWait);
    while(! finished) {
      ResponseEntity<String> resp = this.restTemplate.getForEntity(url, String.class);
      var rbody = resp.getBody();
      try {
        var rmap = mapper.readValue(rbody, HashMap.class);// new TypeReference<HashMap<String,Object>>() {});
        finished = rmap.get("endTime") != null;
      } catch(Exception e) {
        finished = false;
      }
      if (ZonedDateTime.now().compareTo(tend) > 0 ) {
        finished = true;
      }
    }
  }

  @BeforeAll
  static void setEnvironment() {
    headers.setContentType(MediaType.APPLICATION_JSON);
    System.setProperty("ISTIO_HOST", mockServer.getHost());
  }


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
      assertThat(mockServer.isRunning()).isTrue();
      log.debug("mockServer. http://{}:{}/", mockServer.getHost(), mockServer.getServerPort());
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

  @Nested
  class ActivationStarted {
    @Test
    public void activationStartedSimpleSuccess() throws Exception {
      ClassPathResource resource = new ClassPathResource("/body/request/simple/activation_started.json", getClass());
      Helper.TestEvent event = eventBuilder.build();
      log.debug("requestEvent: {}", event.requestEvent);
      RequestEventDto body = mapper.readValue(resource.getInputStream(), RequestEventDto.class);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event.requestEvent, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      assertNotNull(response.getProducts());
      assertEquals(1, response.getProducts().size());
    }

    @Test
    public void activationStartedSimpleFailedNullOrderItems() throws Exception {
      Helper.TestEvent event = eventBuilder.build();
      log.debug("requestEvent: {}", event.requestEvent);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event.requestEvent, headers);
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

    // @Test
    // public void activationStartedBundleSuccess() throws Exception {
    //   HttpEntity<RequestEventDto> request = new HttpEntity<>(body, headers);
    //   ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
    //   log.debug(resp.toString());
    //   assertEquals(HttpStatus.OK, resp.getStatusCode());
    //   ResponseEventDto response = resp.getBody();
    //   assertNotNull(response.getProducts());
    //   assertEquals(1, response.getProducts().size());
    // }
  }
}
