package org.qdrin.qfsm.controller.events;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.mockserver.serialization.HttpRequestSerializer;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.controller.ControllerHelper;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivationAborted extends ControllerHelper {

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
    public void abortSimpleFailedDeclined() throws Exception {
      String offerId = "simpleOffer1";
      OfferDef offerDef = getTestOffers().getOffers().get(offerId);
      Product product = new ProductBuilder("simpleOffer1", "PENDING_ACTIVATE", "simple1-price-trial").build();
      log.debug("product: {}", product);
      JsonNode machineState = buildMachineState("PendingActivate");
      StateMachine<String, String> machine = createMachine(machineState, product);
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
