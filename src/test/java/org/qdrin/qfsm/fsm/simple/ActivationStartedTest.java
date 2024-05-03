package org.qdrin.qfsm.fsm.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@SpringBootTest
public class ActivationStartedTest extends Helper {

  StateMachine<String, String> machine = null;

  @BeforeEach
  public void setup() throws Exception {
    machine = null;
  }

  @AfterEach
  public void tearDown() throws Exception {
    machine.stopReactively().block();
    clearDb();
  }

  @Test
  public void testTrialSuccess() throws Exception {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active").build();
    machine = createMachine(product);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .step()
              .sendEvent(MessageBuilder.withPayload("activation_started")
                  .setHeader("product", product)
                  .setHeader("datetime", OffsetDateTime.now())
                  .build())
              .expectState("PendingActivate")
              .expectStateChanged(1)
              .and()
          .build();
    plan.test();
    assertEquals(product.getStatus(), "PENDING_ACTIVATE");
  }

  @Test
  public void testActivationCompletedSuccess() throws Exception {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    ProductPrice price = product.getProductPrice().get(0);
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    machine = createMachine(product);

    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    List<ActionSuit> deleteActions = (List<ActionSuit>) variables.get("deleteActions");

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .step()
              .sendEvent("activation_started")
              .expectState("PendingActivate")
              .expectStateChanged(1)
              .and()
          .step()
              .sendEvent("activation_completed")
              .expectStates(stateSuit("ActiveTrial", "Paid", "PriceActive"))
              .and()
          .defaultAwaitTime(1)
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    log.debug("actions: {}, deleteActions: {}", actions, deleteActions);
    assertEquals(product.getStatus(), "ACTIVE_TRIAL");
    assert(actions.contains(ActionSuit.PRICE_ENDED));
    assert(! actions.contains(ActionSuit.WAITING_PAY_ENDED));
    assert(! deleteActions.contains(ActionSuit.WAITING_PAY_ENDED));
  }
}
