package org.qdrin.qfsm.controller.common;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.controller.ControllerHelper;
import org.qdrin.qfsm.controllers.EventController;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonTest extends ControllerHelper {

  @BeforeEach
  public void resetMock() {
    eventUrl = String.format("http://localhost:%d%s%s/event", port, basePath, apiVersion);
    // mockServerClient.reset();
    clearDb();
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
