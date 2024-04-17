package org.qdrin.qfsm.web;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.*;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.controllers.EventController;
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
      RequestEventDto body = mapper.readValue(resource.getInputStream(), RequestEventDto.class);
      HttpEntity<RequestEventDto> request = new HttpEntity<>(body, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      assertNotNull(response.getProducts());
      assertEquals(1, response.getProducts().size());
    }
  }

//     @Test
//     public void check411() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       ClassPathResource mockResource = new ClassPathResource("/body/mock/connector/check/Check411.json", getClass());
//       ObjectMapper mapper = new ObjectMapper();
//       String mock = readResourceAsString(mockResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability"))
//         .respond(HttpResponse.response()
//                             .withBody(mock.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
//         );

//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       body.getRelatedParty().setMsisdn("9202599411");
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreq = getMockRequests(null)[0];
//       assertThat(mreq.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreq.getBody();
//       log.debug("mbody: {}", mbody);
//       JsonNode context = mbody.get("context");
//       assertThat(context).isNotNull();

//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertEquals("partner1_paid_code1_month", context.get("partnerServiceId").asText());
//       assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
//       assertEquals(body.getServiceOrderItems().get(0).getAction(), mbody.get("action").asText());
//       assertThat(mbody.get("config").get("command")).isNotNull();
//       assertThat(mbody.get("config").get("command").get("url").asText().contains("availability"));

//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals("102", item0.getReason());
//       assertEquals(411, item0.getPartnerResponse().getResponseCode());
//       assertThat(item0.getInvolvedObjects()).isNotNull();
//       var invObjects = item0.getInvolvedObjects();
//       assertEquals(3, invObjects.size());
//       for(var o: invObjects) {
//         assertThat(o.getServiceId()).isNotNull();
//       }
//       assertEquals("partner1_paid_code1_month", item0.getPartnerServiceId());
//       assertEquals(null, item0.getPartnerProductId());
//     }

//     @Test
//     public void check424() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       ClassPathResource mockResource = new ClassPathResource("/body/mock/connector/check/Check424.json", getClass());
//       ObjectMapper mapper = new ObjectMapper();
//       String mock = readResourceAsString(mockResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability"))
//         .respond(HttpResponse.response()
//                             .withBody(mock.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
//         );    
//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       body.getRelatedParty().setMsisdn("9202599424");
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       var mreq = getMockRequests(null)[0];
//       assertThat(mreq.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreq.getBody();
//       log.debug("mbody: {}", mbody);
//       JsonNode context = mbody.get("context");
//       assertThat(context).isNotNull();

//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertEquals("partner1_paid_code1_month", context.get("partnerServiceId").asText());
//       assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
//       assertEquals(body.getServiceOrderItems().get(0).getAction(), mbody.get("action").asText());
//       assertThat(mbody.get("config").get("command")).isNotNull();
//       assertThat(mbody.get("config").get("command").get("url").asText().contains("availability"));

//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals("999", item0.getReason());
//       assertEquals(424, item0.getPartnerResponse().getResponseCode());
//       assertEquals("partner1_paid_code1_month", item0.getPartnerServiceId());
//       assertEquals(null, item0.getPartnerProductId());
//     }

//     @Test
//     public void check503() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       ClassPathResource mockResource = new ClassPathResource("/body/mock/connector/check/Check503.json", getClass());
//       ObjectMapper mapper = new ObjectMapper();
//       String mock = readResourceAsString(mockResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability"))
//         .respond(HttpResponse.response()
//                             .withBody(mock.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
//         );
//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       var mreq = getMockRequests(null)[0];
//       assertThat(mreq.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreq.getBody();
//       log.debug("mbody: {}", mbody);
//       JsonNode context = mbody.get("context");
//       assertThat(context).isNotNull();

//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertEquals("partner1_paid_code1_month", context.get("partnerServiceId").asText());
//       assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
//       assertEquals(body.getServiceOrderItems().get(0).getAction(), mbody.get("action").asText());
//       assertThat(mbody.get("config").get("command")).isNotNull();
//       assertThat(mbody.get("config").get("command").get("url").asText().contains("availability"));

//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertThat(item0.getReason()).isNull();
//       assertThat(item0.getPartnerResponse()).isNotNull();
//       assertEquals(503, item0.getPartnerResponse().getResponseCode());
//       assertEquals("Partner Error", item0.getError().getErrorCode());
//       assertEquals("partner1_paid_code1_month", item0.getPartnerServiceId());
//       assertEquals(null, item0.getPartnerProductId());
//     }

//     @Test
//     public void check502() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       ClassPathResource mockResource = new ClassPathResource("/body/mock/connector/check/Check502.json", getClass());
//       ObjectMapper mapper = new ObjectMapper();
//       String mock = readResourceAsString(mockResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability"))
//         .respond(HttpResponse.response()
//                             .withBody(mock.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
//         );
//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       body.getRelatedParty().setMsisdn("9202599502");
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertThat(item0.getReason()).isNull();
//       assertThat(item0.getPartnerResponse()).isNotNull();
//       assertEquals(502, item0.getPartnerResponse().getResponseCode());
//       assertEquals("Partner Error", item0.getError().getErrorCode());
//       assertEquals("partner1_paid_code1_month", item0.getPartnerServiceId());
//       assertEquals(null, item0.getPartnerProductId());
//     }

//     @Test
//     public void checkMulti() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccessMulti.json", getClass());
//       ClassPathResource mockResource = new ClassPathResource("/body/mock/connector/check/CheckSuccess.json", getClass());
//       String mock = readResourceAsString(mockResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability")
//         )  // .withBody(mockRequest))
//         .respond(HttpResponse.response()
//                             .withBody(mock.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
//         );

//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(2, orderItemsResults.size());
//       var mreq = getMockRequests(null);
//       // log.debug("mock requests: {}", mreq);
//       assertEquals(1, mreq.length);
      
//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals("0", item0.getReason());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());
//       assertEquals("partner1_paid_code1_month", item0.getPartnerServiceId());
//       assertEquals(null, item0.getPartnerProductId());

//       // check second item. It should have GetConfigurationError
//       var item1 = orderItemsResults.get(1);
//       assertEquals("2", item1.getId());
//       assertEquals("Error", item1.getResult());
//       assertEquals("Configuration Error", item1.getError().getErrorCode());
//     }

//     @Test
//     public void checkNoOffer() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());

//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       body.getServiceOrderItems().get(0).setServiceId("UndefinedOfferId");
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
      
//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertThat(item0.getError()).isNotNull();
//       assertEquals("Configuration Error", item0.getError().getErrorCode());
//       assertThat(item0.getReason()).isNull();
//       assertThat(item0.getPartnerResponse()).isNull();
//     }

//     @Test
//     public void checkConnectFailed() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       ObjectMapper mapper = new ObjectMapper();
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       body.getServiceOrderItems().get(0).setServiceId("fpc_partner1_offer1_fake");
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       // check first item. It should be "OK"
//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals("ConnectorError", item0.getError().getErrorCode());
//       assertThat(item0.getPartnerResponse()).isNull();
//     }

//     @Test
//     public void checkBadConnectorResponse() throws Exception {
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/availability"))
//         .respond(HttpResponse.response().withBody("{\"Status\":\"Up\"}"));

//       ClassPathResource resource = new ClassPathResource("/body/request/CheckSuccess.json", getClass());
//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       ObjectMapper mapper = new ObjectMapper();
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertThat(item0.getReason()).isNull();
//       assertThat(item0.getError()).isNotNull();
//       assertEquals("Partner Error", item0.getError().getErrorCode());
//     }

//     @Test
//     public void checkExtraPartnerServices() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/CheckExtraPartnerServices.json", getClass());


//       String url = String.format("http://localhost:%d%s%s/process/start/sync", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<OrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       var item0 = orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals("ValidationError", item0.getError().getErrorCode());
//     }
//   }

//   @Nested
//   class AvailableServicesMethod {
//     private List<RequestDefinition> setMocks(String correlationId, String gbody, String obody, String pbody, String checkBody) {
//       var res = new ArrayList<RequestDefinition>();
//       var reqGroup = HttpRequest.request()
//         .withMethod("POST")
//         .withPath("/eapi/factory-adapter-bss-connector/v1/fpcGroup")
//         .withLogCorrelationId(correlationId);
//       res.add(reqGroup);
//       var reqOffer = HttpRequest.request()
//         .withMethod("POST")
//         .withPath("/eapi/factory-adapter-bss-connector/v1/fpcOffers")
//         .withLogCorrelationId(correlationId);
//       res.add(reqOffer);
//       var reqPoq = HttpRequest.request()
//         .withMethod("POST")
//         .withPath("/eapi/factory-adapter-bss-connector/v1/poq")
//         .withLogCorrelationId(correlationId);
//       res.add(reqPoq);
//       var reqPrice = HttpRequest.request()
//               .withMethod("POST")
//               .withPath("/eapi/factory-adapter-bss-connector/v1/price")
//               .withLogCorrelationId(correlationId);
//       res.add(reqPrice);
//       mockServerClient
//         .when(reqGroup)
//         .respond(HttpResponse.response()
//                             .withBody(gbody)
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(reqOffer)
//         .respond(HttpResponse.response()
//                             .withBody(obody)
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(reqPoq)
//         .respond(HttpResponse.response()
//                             .withBody(pbody)
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//               .when(reqPrice)
//               .respond(HttpResponse.response()
//                       .withBody(checkBody)
//                       .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       return res;
//     }

//     @Test
//     public void availableServicesSuccess() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuccess.json", getClass());
//       ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource).toString();
//       String fpcOffers = readResourceAsString(fpcOffersResource).toString();
//       String poq = readResourceAsString(poqResource).toString();
//       String price = readResourceAsString(checkPriceResource).toString();
//       var mock_reqs = setMocks("availableServicesSuccess", fpcGroup, fpcOffers, poq, price);

//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getClazz()).isNull();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       log.debug("m0ck ," + mreqs);
//       assertEquals(3, mreqs.length);
      
//       // Check FpcGroup request params
//       var mreqGroup = mreqs[0];
//       assertThat(mreqGroup.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreqGroup.getBody();
//       log.debug("mbodyGroup: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       var context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       var requestObjects = context.get("requestObjects");
//       assertEquals(1, requestObjects.size());
//       assertEquals(body.getServiceOrderItems().get(0).getServiceId(), requestObjects.get(0).get("id").asText());

//       var mreqOffer = mreqs[1];
//       var fpcGroupJson = Spin.JSON(fpcGroup);
//       mbody = (JsonBody) mreqOffer.getBody();
//       log.debug("mbodyOffer: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       requestObjects = context.get("requestObjects");
//       log.debug("fpcOffer requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
//       var reqGroups = fpcGroupJson.prop("items").elements().get(0).prop("items").elements();
//       assertEquals(reqGroups.size(), requestObjects.size());
//       assertEquals(reqGroups.get(0).prop("id").stringValue(), requestObjects.get(0).get("id").asText());

//       var mreqPoq = mreqs[2];
//       var poqJson = Spin.JSON(poq);
//       mbody = (JsonBody) mreqPoq.getBody();
//       log.debug("mbodyPoq: {}", mbody);
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       requestObjects = context.get("requestObjects");
//       log.debug("poq requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
//       assertThat(context.get("partnerParty")).isNotNull();
//       assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
//       var reqOffers = poqJson.prop("items").elements();
//       assertEquals(reqOffers.size(), requestObjects.size());
//       for(var offer: reqOffers) {
//         var offerId = offer.prop("id").stringValue();
//         boolean hasId = false;
//         for(var requestObject: requestObjects) {
//           if(requestObject.get("id").asText().equals(offerId)) {
//             hasId = true;
//           }
//         }
//         assertThat(hasId);
//       }

// //      var mreqPrice = mreqs[3];
// //      var priceJson = Spin.JSON(price);
// //      mbody = (JsonBody) mreqPrice.getBody();
// //      log.debug("mbodyprice: {}", mbody);
// //      assertThat(mbody.get("order")).isNotNull();
// //      assertThat(mbody.get("order").get("internalId")).isNotNull();
// //      assertThat(mbody.get("context")).isNotNull();
// //      context = mbody.get("context");
// //      assertThat(context.get("requestObjects")).isNotNull();
// //      requestObjects = context.get("requestObjects");
// //      log.debug("poq requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
// //      assertThat(context.get("partnerParty")).isNotNull();
// //      assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());


//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var pitems = item0.getItems();
//       assertEquals(2, pitems.size());
//       var pitem0 = pitems.get(0);
//       var pitem1 = pitems.get(1);
//       assertEquals("2fb5ea06-1a1f-494a-94c4-724ba910d8bb", pitem0.getId());
//       assertEquals("productOffering", pitem0.getType());
//       // Должны убрать выравниающую цену
//       assertEquals(reqOffers.get(0).prop("prices").elements().size()-1, pitem0.getPrices().size());
//       assertEquals("3354dd60-884b-472d-a434-cc75715baf6e", pitem1.getId());
//       assertEquals("productOffering", pitem1.getType());
//       assertEquals(reqOffers.get(1).prop("prices").elements().size()-1, pitem1.getPrices().size());
//     }

//     @Test
//     public void availableServicesSuspiciousPoqResponse() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuspicious.json", getClass());
//       ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource);
//       String fpcOffers = readResourceAsString(fpcOffersResource);
//       String poq = readResourceAsString(poqResource);
//       String price = readResourceAsString(checkPriceResource).toString();
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcGroup")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcGroup.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcOffers")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcOffers.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/poq")
//         ).respond(HttpResponse.response()
//                             .withBody(poq.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));


//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       assertEquals(3, mreqs.length);
      
//       // Check FpcGroup request params
//       var mreqGroup = mreqs[0];
//       assertThat(mreqGroup.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreqGroup.getBody();
//       log.debug("mbodyGroup: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       var context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       var requestObjects = context.get("requestObjects");
//       assertEquals(1, requestObjects.size());
//       assertEquals(body.getServiceOrderItems().get(0).getServiceId(), requestObjects.get(0).get("id").asText());

//       var mreqOffer = mreqs[1];
//       var fpcGroupJson = Spin.JSON(fpcGroup);
//       mbody = (JsonBody) mreqOffer.getBody();
//       log.debug("mbodyOffer: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       requestObjects = context.get("requestObjects");
//       log.debug("fpcOffer requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
//       var reqGroups = fpcGroupJson.prop("items").elements().get(0).prop("items").elements();
//       assertEquals(reqGroups.size(), requestObjects.size());
//       assertEquals(reqGroups.get(0).prop("id").stringValue(), requestObjects.get(0).get("id").asText());

//       var mreqPoq = mreqs[2];
//       var poqJson = Spin.JSON(poq);
//       mbody = (JsonBody) mreqPoq.getBody();
//       log.debug("mbodyPoq: {}", mbody);
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       context = mbody.get("context");
//       assertThat(context.get("requestObjects")).isNotNull();
//       requestObjects = context.get("requestObjects");
//       log.debug("poq requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
//       assertThat(context.get("partnerParty")).isNotNull();
//       assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
//       var reqOffers = poqJson.prop("items").elements();
//       assertEquals(reqOffers.size(), requestObjects.size());
//       for(var offer: reqOffers) {
//         var offerId = offer.prop("id").stringValue();
//         boolean hasId = false;
//         for(var requestObject: requestObjects) {
//           if(requestObject.get("id").asText().equals(offerId)) {
//             hasId = true;
//           }
//         }
//         assertThat(hasId);
//       }

//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var pitems = item0.getItems();
//       assertEquals(2, pitems.size());
//       var pitem0 = pitems.get(0);
//       var pitem1 = pitems.get(1);
//       assertEquals("2fb5ea06-1a1f-494a-94c4-724ba910d8bb", pitem0.getId());
//       assertEquals("ProductOffering", pitem0.getType());
//       // Должны убрать выравниающую цену
//       assertEquals(reqOffers.get(0).prop("prices").elements().size(), pitem0.getPrices().size());
//       assertEquals("3354dd60-884b-472d-a434-cc75715baf6e", pitem1.getId());
//       assertEquals("ProductOffering", pitem1.getType());
//       assertEquals(reqOffers.get(1).prop("prices").elements().size(), pitem1.getPrices().size());

//     }

//     @Test
//     public void availableServicesBadPoqResponse() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource);
//       String fpcOffers = readResourceAsString(fpcOffersResource);
//       String poq = readResourceAsString(poqResource)
//                   .replace("\"type\": \"AlignmentCharge\"", "\"type_fake\": \"AlignmentCharge\"");
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcGroup")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcGroup.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcOffers")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcOffers.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/poq")
//         ).respond(HttpResponse.response()
//                             .withBody(poq.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));

//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       assertEquals(3, mreqs.length);
//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());
//       var error = item0.getError();
//       assertThat(error).isNotNull();
//       assertEquals("PartnerError", error.getErrorCode());

//       var pitems = item0.getItems();
//       assertThat(pitems).isNull();
//     }

//     @Test
//     public void availableServicesEmptyFpcResponse() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/ConnectorEmptyResponse.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuccess.json", getClass());
//       ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource).toString();
//       String fpcOffers = readResourceAsString(fpcOffersResource).toString();
//       String poq = readResourceAsString(poqResource).toString();
//       String price = readResourceAsString(checkPriceResource).toString();
//       var mock_reqs = setMocks("availableServicesSuccess", fpcGroup, fpcOffers, poq, price);
//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getClazz()).isNull();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       assertEquals(1, mreqs.length);
      
//       // Check FpcGroup request params
//       var mreqGroup = mreqs[0];
//       assertThat(mreqGroup.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreqGroup.getBody();
//       log.debug("mbodyGroup: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       assertThat(mbody.get("context").get("requestObjects")).isNotNull();
//       var requestObjects = mbody.get("context").get("requestObjects");
//       assertEquals(1, requestObjects.size());
//       assertEquals(body.getServiceOrderItems().get(0).getServiceId(), requestObjects.get(0).get("id").asText());


//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var error = item0.getError();
//       assertThat(error).isNotNull();
//       assertEquals("EmptyConnectorResponse", error.getErrorCode());
//       assertThat(error.getUserMessage().contains("/eapi/factory-adapter-bss-connector/v1/fpcGroup"));
//     }

//     @Test
//     public void availableServicesEmptyFpcOfferResponse() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/ConnectorEmptyResponse.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuccess.json", getClass());
//       ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource).toString();
//       String fpcOffers = readResourceAsString(fpcOffersResource).toString();
//       String poq = readResourceAsString(poqResource).toString();
//       String price = readResourceAsString(poqResource).toString();
//       var mock_reqs = setMocks("availableServicesSuccess", fpcGroup, fpcOffers, poq, price);

//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getClazz()).isNull();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       assertEquals(2, mreqs.length);
      
//       // Check FpcGroup request params
//       var mreqGroup = mreqs[0];
//       assertThat(mreqGroup.getBody()).isNotNull();
//       JsonBody mbody = (JsonBody) mreqGroup.getBody();
//       log.debug("mbodyGroup: {}", mbody);
//       assertEquals(body.getServiceOrderItems().get(0).getId(), mbody.get("orderItemId").asText());
//       assertThat(mbody.get("order")).isNotNull();
//       assertThat(mbody.get("order").get("internalId")).isNotNull();
//       assertThat(mbody.get("context")).isNotNull();
//       assertThat(mbody.get("context").get("requestObjects")).isNotNull();
//       var requestObjects = mbody.get("context").get("requestObjects");
//       assertEquals(1, requestObjects.size());
//       assertEquals(body.getServiceOrderItems().get(0).getServiceId(), requestObjects.get(0).get("id").asText());


//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var error = item0.getError();
//       assertThat(error).isNotNull();
//       assertEquals("EmptyConnectorResponse", error.getErrorCode());
//       assertThat(error.getUserMessage().contains("/eapi/factory-adapter-bss-connector/v1/fpcOffer"));
//     }

//     @Test
//     public void availableServicesEmptyPoqResponse() throws Exception {
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/ConnectorEmptyResponse.json", getClass());
//       ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource).toString();
//       String fpcOffers = readResourceAsString(fpcOffersResource).toString();
//       String poq = readResourceAsString(poqResource).toString();
//       String price = readResourceAsString(checkPriceResource).toString();
//       var mock_reqs = setMocks("availableServicesSuccess", fpcGroup, fpcOffers, poq, price);

//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getClazz()).isNull();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());
//       var mreqs = getMockRequests(null);
//       assertEquals(3, mreqs.length);
      
//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("Error", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var error = item0.getError();
//       assertThat(error).isNotNull();
//       assertEquals("EmptyConnectorResponse", error.getErrorCode());
//       assertThat(error.getUserMessage().contains("/eapi/factory-adapter-bss-connector/v1/poq"));
//     }

// //  @Test
// //  public void  checkPrice() throws Exception {
// //    ClassPathResource resource = new ClassPathResource("/body/request/CheckPriceValueRequestSuccess.json", getClass());
// //    ClassPathResource checkPriceResource = new ClassPathResource("/body/mock/connector/availability/CheckPriceValueSuccess.json", getClass());
// //    ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
// //    ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
// //    ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/ConnectorEmptyResponse.json", getClass());
// //    String fpcGroup = readResourceAsString(fpcGroupResource).toString();
// //    String fpcOffers = readResourceAsString(fpcOffersResource).toString();
// //    String poq = readResourceAsString(poqResource).toString();
// //    String price = readResourceAsString(checkPriceResource).toString();
// //    var mock_reqs = setMocks("availableServicesSuccess", fpcGroup, fpcOffers, poq, price);
// //    log.debug("m0ck1 , " +  mock_reqs);
// //    String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
// //    log.debug(url);
// //    StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
// //    log.debug("body ," + body);
// //    HttpHeaders headers = new HttpHeaders();
// //    headers.setContentType(MediaType.APPLICATION_JSON);
// //    HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
// //    ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
// //    log.debug(resp.toString());
// //    assertEquals(HttpStatus.OK, resp.getStatusCode());
// //    var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
// //    assertThat(rbody.getOrder().getInternalId()).isNotNull();
// //    assertThat(rbody.getResult()).isNotNull();
// //    var result = rbody.getResult();
// //    assertThat(result.getClazz()).isNull();
// //    assertThat(result.getOrderItemsResults()).isNotNull();
// //    var orderItemsResults = result.getOrderItemsResults();
// //    assertEquals(1, orderItemsResults.size());
// //    var mreqs = getMockRequests(null);
// //    log.debug("mr3qs ," + mreqs);
// //    //assertEquals(3, mreqs.length);
// //
// //    var mreqPrice = mreqs[0];
// //    var priceJson = Spin.JSON(mreqPrice);
// //    JsonBody mbody = (JsonBody) mreqPrice.getBody();
// //    log.debug("mbodyprice: {}", mbody);
// //    assertThat(mbody.get("order")).isNotNull();
// //    assertThat(mbody.get("order").get("internalId")).isNotNull();
// //    assertThat(mbody.get("context")).isNotNull();
// //    var context = mbody.get("context");
// //    assertThat(context.get("requestObjects")).isNotNull();
// //    var requestObjects = context.get("requestObjects");
// //    String testVar1 = String.valueOf(requestObjects.get(0).get("productOfferingId"));
// //    String testVar2 = String.valueOf(requestObjects.get("productOfferingPrice"));
// //    log.debug("testVar1 ," + testVar1.getClass());
// //    String testVarExpected1 = "productOfferingId_bss_multimapper";
// //    log.debug("testVarExpected1 ," + testVarExpected1.getClass());
// //    //log.debug(testVar1.equals(testVarExpected1));
// //    String testVarExpected2 = "productOfferingPriceId_bss_multimapper";
// //    log.debug("CheckPrice requestObjects: {} ({})", requestObjects, requestObjects.getClass().getName());
// //    assertThat(context.get("partnerParty")).isNotNull();
// //    assertEquals(body.getRelatedParty().getMsisdn(), context.get("partnerParty").get("msisdn").asText());
// //    //assertEquals(testVarExpected1, testVar1);
// //    assertThat(testVarExpected1.equals(testVar1)).isTrue();
// //    assertThat(testVarExpected2.equals(testVar2)).isTrue();
// //
// //  }
// }

//   @Nested
//   class LoadTests {
//     //@RepeatedTest(10)
//     @RepeatedTest(6)
//     @Execution(ExecutionMode.CONCURRENT)
//     public void availableServicesLoad() throws Exception {
//       int testIndex = testCounter.incrementAndGet();
//       log.debug("availableServicesMultiLoad {} start", testIndex);
//       ClassPathResource resource = new ClassPathResource("/body/request/AvailabilitySuccess.json", getClass());
//       ClassPathResource fpcGroupResource = new ClassPathResource("/body/mock/connector/availability/FpcGroupSuccess.json", getClass());
//       ClassPathResource fpcOffersResource = new ClassPathResource("/body/mock/connector/availability/FpcOffersSuccess.json", getClass());
//       ClassPathResource poqResource = new ClassPathResource("/body/mock/connector/availability/PoqSuccess.json", getClass());
//       String fpcGroup = readResourceAsString(fpcGroupResource);
//       String fpcOffers = readResourceAsString(fpcOffersResource);
//       String poq = readResourceAsString(poqResource);
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcGroup")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcGroup.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/fpcOffers")
//         ).respond(HttpResponse.response()
//                             .withBody(fpcOffers.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));
//       mockServerClient
//         .when(HttpRequest.request().withMethod("POST").withPath("/eapi/factory-adapter-bss-connector/v1/poq")
//         ).respond(HttpResponse.response()
//                             .withBody(poq.toString())
//                             .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON));

//       String url = String.format("http://localhost:%d%s%s/process/start/availableServices", port, basePath, apiVersion);
//       log.debug(url);
//       StartSyncRequestDto body = mapper.readValue(resource.getInputStream(), StartSyncRequestDto.class);
//       HttpHeaders headers = new HttpHeaders();
//       headers.setContentType(MediaType.APPLICATION_JSON);
//       HttpEntity<StartSyncRequestDto> request = new HttpEntity<>(body, headers);
//       var t0 = System.currentTimeMillis();
//       ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);
//       var t1 = System.currentTimeMillis();
//       log.debug(resp.toString());
//       assertEquals(HttpStatus.OK, resp.getStatusCode());
//       var rbody = mapper.readValue(resp.getBody(), new TypeReference<StartResponseDto<AvailableOrderItemResult>>() {});
//       assertThat(rbody.getOrder().getInternalId()).isNotNull();
//       assertThat(rbody.getResult()).isNotNull();
//       var result = rbody.getResult();
//       assertThat(result.getOrderItemsResults()).isNotNull();
//       var orderItemsResults = result.getOrderItemsResults();
//       assertEquals(1, orderItemsResults.size());

//       var item0 = (AvailableOrderItemResult) orderItemsResults.get(0);
//       assertEquals("1", item0.getId());
//       assertEquals("OK", item0.getResult());
//       assertEquals(200, item0.getPartnerResponse().getResponseCode());

//       var pitems = item0.getItems();
//       assertEquals(2, pitems.size());
//       log.debug("availableServicesMultiLoad {} finish: {}", testIndex, t1 - t0);
//     }
//   }
}
