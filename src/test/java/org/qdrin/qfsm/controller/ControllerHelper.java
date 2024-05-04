package org.qdrin.qfsm.controller;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.controllers.EventController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class ControllerHelper extends Helper {

  public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
      .parse("mockserver/mockserver")
      .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

  static {
    mockServer.start();
  }

  protected static MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());

  static HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer(null);

  public static HttpRequest[] getMockRequests(HttpRequest request) {
    var mockRequestsString = mockServerClient.retrieveRecordedRequests(request, Format.JSON);
    var mreq = httpRequestSerializer.deserializeArray(mockRequestsString);
    return mreq;
  }

  @Autowired
  protected EventController eventController;

  @Autowired
  protected TestRestTemplate restTemplate;

  @Value(value="${local.server.port}")
  protected int port;

  @Value(value="${server.servlet.context-path}")
  protected String basePath;

  @Value(value="${management.endpoints.web.base-path}")
  protected String managePath;

  protected static HttpHeaders headers = getHeaders();

  protected String apiVersion = "/v1";
  protected String eventUrl;
}
