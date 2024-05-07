package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationAbortedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  @Nested
  class Simple {
    @Test
    public void testActiveTrial() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-trial")
        .productStartDate(t0)
        .tarificationPeriod(0)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .build();
      ProductPrice price = bundle.drive.getProductPrice().get(0);
      price.setNextPayDate(t1);
      price.setPeriod(0);
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .status("ABORTED")
        .tarificationPeriod(0)
        .pricePeriod(0)
        .build();
      machine = createMachine(bundle);

      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .sendEvent("activation_aborted")
                .expectStates(Helper.stateSuit("Aborted"))
                .and()
            .build();
      plan.test();
      log.debug("states: {}", machine.getState().getIds());
      Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
    }

    @Test
    public void testActive() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
        .productStartDate(t0)
        .tarificationPeriod(0)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .build();
      ProductPrice price = bundle.drive.getProductPrice().get(0);
      price.setNextPayDate(t1);
      price.setPeriod(0);
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .status("ABORTED")
        .tarificationPeriod(0)
        .pricePeriod(0)
        .build();
      machine = createMachine(bundle);

      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .sendEvent("activation_aborted")
                .expectStates(Helper.stateSuit("Aborted"))
                .and()
            .build();
      plan.test();
      log.debug("states: {}", machine.getState().getIds());
      Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
    }

    @Test
    public void testActiveNoNextPayDate() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
        .tarificationPeriod(0)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .build();

      TestBundle expectedBundle = new BundleBuilder(bundle)
        .status("ACTIVE")
        .tarificationPeriod(0)
        .pricePeriod(0)
        .activeEndDate(null)
        .build();
      
      machine = createMachine(bundle);
      
      log.debug("start. actions: {}", machine.getExtendedState().getVariables().get("actions"));
      List<ActionSuite> expectedActions = Arrays.asList(ActionSuite.WAITING_PAY_ENDED);
      List<ActionSuite> expectedDeleteActions = new  ArrayList<>();
      OffsetDateTime expectedWaitingPayEnded = t0.plus(getWaitingPayInterval());

      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .sendEvent("activation_completed")
                .expectStates(Helper.stateSuit("Active", "WaitingPayment", "PriceWaiting"))
                .expectVariable("deleteActions", expectedDeleteActions)
                .expectVariable("actions", expectedActions)
                .and()
            .build();
      plan.test();
      log.debug("states: {}", machine.getState().getIds());
      Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
      
      Map<Object, Object> variables = machine.getExtendedState().getVariables(); 
      List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
      OffsetDateTime waitingPayEnded = actions.get(0).getWakeAt();
      assertDates(expectedWaitingPayEnded, waitingPayEnded);
    }
  }
}
