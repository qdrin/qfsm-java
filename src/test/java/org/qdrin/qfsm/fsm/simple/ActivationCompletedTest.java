package org.qdrin.qfsm.fsm.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

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
public class ActivationCompletedTest {

  @Autowired
  Helper helper;

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    helper.clearDb();
  }

  @Test
  public void testActiveTrialSuccess() throws Exception {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    ProductPrice price = product.getProductPrice().get(0);
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    machine = helper.createMachine(null, product);
    
    List<ActionSuit> expectedActions = Arrays.asList(ActionSuit.PRICE_ENDED);
    List<ActionSuit> expectedDeleteActions = new  ArrayList<>();

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
              .expectStates(Helper.stateSuit("ActiveTrial", "Paid", "PriceActive"))
              .expectVariable("deleteActions", expectedDeleteActions)
              .expectVariable("actions", expectedActions)
              .and()
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    assertEquals(product.getStatus(), "ACTIVE_TRIAL");
    assertEquals(price.getNextPayDate(), product.getActiveEndDate());
    Map<Object, Object> variables = machine.getExtendedState().getVariables(); 
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    assertEquals(product.getActiveEndDate().minus(helper.getPriceEndedBefore()),
                  actions.get(0).getWakeAt());
  }

  @Test
  public void testActiveSuccess() throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active")
      .tarificationPeriod(0)
      .build();
    ProductPrice price = product.getProductPrice().get(0);
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    machine = helper.createMachine(Helper.buildMachineState("PendingActivate"), product);
    
    log.debug("start. actions: {}", machine.getExtendedState().getVariables().get("actions"));
    List<ActionSuit> expectedActions = Arrays.asList(ActionSuit.WAITING_PAY_ENDED, ActionSuit.PRICE_ENDED);
    List<ActionSuit> expectedDeleteActions = new  ArrayList<>();
    OffsetDateTime expectedWaitingPayEnded = t0.plus(helper.getWaitingPayInterval());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("activation_completed")
              .expectStates(Helper.stateSuit("Active", "WaitingPayment", "PriceActive"))
              .expectVariable("deleteActions", expectedDeleteActions)
              .expectVariable("actions", expectedActions)
              .and()
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    assertEquals(product.getStatus(), "ACTIVE");
    assertEquals(price.getNextPayDate(), product.getActiveEndDate());
    Map<Object, Object> variables = machine.getExtendedState().getVariables(); 
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    OffsetDateTime waitingPayEnded = actions.get(0).getWakeAt();
    OffsetDateTime priceEnded = actions.get(1).getWakeAt();
    assertEquals(priceEnded, price.getNextPayDate().minus(helper.getPriceEndedBefore()));
    assert(waitingPayEnded.isAfter(expectedWaitingPayEnded.minusSeconds(5)));
    assert(waitingPayEnded.isBefore(expectedWaitingPayEnded.plusSeconds(5)));
  }
}
