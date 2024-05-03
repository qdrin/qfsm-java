package org.qdrin.qfsm.controller;

import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Format;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.Helper;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

public class ControllerHelper extends Helper {

  public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
      .parse("mockserver/mockserver")
      .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

  static {
    mockServer.start();
  }

  protected static MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
  private
  @AfterAll
  static void stopContainers() {
    mockServer.stop();
  }

  static HttpRequestSerializer httpRequestSerializer = new HttpRequestSerializer(null);

  public static MockServerClient getMockServerClient() {
    return new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
  }

  public static HttpRequest[] getMockRequests(HttpRequest request) {
    var mockRequestsString = mockServerClient.retrieveRecordedRequests(request, Format.JSON);
    var mreq = httpRequestSerializer.deserializeArray(mockRequestsString);
    return mreq;
  }

  // @Autowired
  // private static EventController eventController;
}
