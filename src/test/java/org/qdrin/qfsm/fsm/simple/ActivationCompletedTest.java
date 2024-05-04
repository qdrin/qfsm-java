package org.qdrin.qfsm.fsm.simple;

import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  @Nested
  class Simple {
    @Test
    public void testActiveTrialSuccess() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      Product product = new ProductBuilder("simpleOffer1", "PENDING_ACTIVATE", "simple1-price-trial")
        .productStartDate(t0)  // cause we startin product now
        .build();
      ProductPrice price = product.getProductPrice().get(0);
      price.setNextPayDate(t1);
      price.setPeriod(0);
      Product expectedProduct = new ProductBuilder(product)
        .status("ACTIVE_TRIAL")
        .tarificationPeriod(1)
        .pricePeriod(1)
        .trialEndDate(t1)
        .activeEndDate(t1)
        .build();
      machine = createMachine(null, product);
      
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
      Helper.Assertions.assertProductEquals(expectedProduct, product);
    }

    @Test
    public void testActiveSuccess() throws Exception {
      Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active")
        .tarificationPeriod(0)
        .build();
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      ProductPrice price = product.getProductPrice().get(0);
      price.setNextPayDate(t1);

      Product expectedProduct = new ProductBuilder(product)
        .status("ACTIVE")
        .tarificationPeriod(0)
        .pricePeriod(1)
        .activeEndDate(t1)
        .build();
      
      machine = createMachine(Helper.buildMachineState("PendingActivate"), product);
      
      log.debug("start. actions: {}", machine.getExtendedState().getVariables().get("actions"));
      List<ActionSuit> expectedActions = Arrays.asList(ActionSuit.WAITING_PAY_ENDED, ActionSuit.PRICE_ENDED);
      List<ActionSuit> expectedDeleteActions = new  ArrayList<>();
      OffsetDateTime expectedWaitingPayEnded = t0.plus(getWaitingPayInterval());

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
      Helper.Assertions.assertProductEquals(expectedProduct, product);
      
      Map<Object, Object> variables = machine.getExtendedState().getVariables(); 
      List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
      OffsetDateTime waitingPayEnded = actions.get(0).getWakeAt();
      OffsetDateTime priceEnded = actions.get(1).getWakeAt();
      assertEquals(priceEnded, price.getNextPayDate().minus(getPriceEndedBefore()));
      assert(waitingPayEnded.isAfter(expectedWaitingPayEnded.minusSeconds(5)));
      assert(waitingPayEnded.isBefore(expectedWaitingPayEnded.plusSeconds(5)));
    }
  }
}
