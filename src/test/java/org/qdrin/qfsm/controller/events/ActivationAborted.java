package org.qdrin.qfsm.controller.events;

import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;
import java.util.*;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.Helper.buildMachineState;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.controller.ControllerStarter;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;

import com.fasterxml.jackson.databind.JsonNode;
import static org.qdrin.qfsm.Helper.Assertions.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActivationAborted extends ControllerStarter {

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
    public void abortSimpleDeclined() throws Exception {
      String offerId = "simpleOffer1";
      String priceId = "simple1-price-trial";
      OfferDef offerDef = Helper.testOffers.getOffers().get(offerId);

      OffsetDateTime t0 = OffsetDateTime.now();
      JsonNode machineState = buildMachineState("PendingActivate");
      TestBundle bundle = new BundleBuilder(offerId, priceId)
        .machineState(machineState)
        .tarificationPeriod(0)
        .save(productRepository)
        .build();
      StateMachine<String, String> machine = createMachine(bundle);
      RequestEventDto event = new EventBuilder("activation_aborted", bundle.drive).build();
      HttpEntity<RequestEventDto> request = new HttpEntity<>(event, headers);
      ResponseEntity<ResponseEventDto> resp = restTemplate.postForEntity(eventUrl, request, ResponseEventDto.class);
      log.debug(resp.toString());
      assertEquals(HttpStatus.OK, resp.getStatusCode());
      ResponseEventDto response = resp.getBody();
      assertEquals(event.getEvent().getRefId(), response.getRefId());
      assertResponseEquals(event, response);
      List<ProductResponseDto> resultProducts = response.getProducts();
      assertEquals(bundle.drive.getProductId(), resultProducts.get(0).getProductId());
      assertEquals("ABORTED", resultProducts.get(0).getStatus());
      assertEquals(offerId, resultProducts.get(0).getProductOfferingId());
      assertEquals(offerDef.getName(), resultProducts.get(0).getProductOfferingName());

      Product product = getProduct(resultProducts.get(0).getProductId());
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .productIds(Arrays.asList(product))
        .machineState(buildMachineState("Aborted"))
        .status("ABORTED")
        .tarificationPeriod(0)
        .build();
      log.debug("expected: {}\nactual: {}", expectedBundle.drive, product);
      assertProductEquals(expectedBundle.drive, product);
    }
  }

  @Nested
  class Bundle {

  }
}
