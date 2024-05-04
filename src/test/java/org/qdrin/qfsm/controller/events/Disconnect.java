package org.qdrin.qfsm.controller.events;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.controller.ControllerHelper;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

// @SpringBootTest(webEnvironment =  WebEnvironment.RANDOM_PORT)
@Slf4j
public class Disconnect extends ControllerHelper {

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

  @Nested
  class Bundle {
    
  }
}
