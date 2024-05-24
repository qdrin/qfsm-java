package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.Arrays;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.Helper.Assertions.*;
import static org.qdrin.qfsm.Helper.buildMachineState;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendEndedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  public static Stream<Arguments> testSuccess() {
    List<String> empty = new ArrayList<>();
    List<String> components = Arrays.asList("component1", "component2", "component3");
    return Stream.of(  // TODO: expand list of cases
      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceActive"), components),

      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanging"), components),

      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceChanged"), components),

      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), components)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testSuccess(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {

    OffsetDateTime t0 = OffsetDateTime.now();
    String usageState = states.get(0);
    if(usageState.contains("Active")) {
      usageState = priceId.contains("trial") ? "ActiveTrial" : "Active";
    }
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
    status = Arrays.asList("Suspended", "Resuming").contains(usageState) ? "SUSPEND" : status;

    String priceState = states.get(2);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .tarificationPeriod(2)
      .activeEndDate(t0.plusDays(15))
      .pricePeriod(2)
      .status(status)
      .machineState(buildMachineState(states))
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    String productId = bundle.drive.getProductId();
    List<String> expectedStates = Arrays.asList(usageState, "PaymentStopping", priceState);
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(status)
      .machineState(buildMachineState(expectedStates))
      .build();
    machine = createMachine(bundle);
    
    OffsetDateTime taskDate = t0.plus(getWaitingPayInterval());
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.DISCONNECT_EXTERNAL_EXTERNAL).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("suspend_ended")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  public static Stream<Arguments> testRejected() {
    List<String> empty = new ArrayList<>();
    List<String> components = Arrays.asList("component1", "component2", "component3");
    return Stream.of(  // TODO: expand list of cases
      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Active", "WaitingPayment", "PriceChanging"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("ActiveTrial", "WaitingPayment", "PriceChanged"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Active", "WaitingPayment", "PriceWaiting"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Active", "WaitingPayment", "PriceWaiting"), components),
      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList("Prolongation", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList("Active", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("ActiveTrial", "Paid", "PriceChanging"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("Active", "Paid", "PriceChanged"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("ActiveTrial", "Paid", "PriceWaiting"), components)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testRejected(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {

    OffsetDateTime t0 = OffsetDateTime.now();
    String usageState = states.get(0);
    if(usageState.contains("Active")) {
      usageState = priceId.contains("trial") ? "ActiveTrial" : "Active";
    }
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
    status = Arrays.asList("Suspended", "Resuming").contains(usageState) ? "SUSPEND" : status;

    String priceState = states.get(2);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .tarificationPeriod(2)
      .activeEndDate(t0.plusDays(15))
      .pricePeriod(2)
      .status(status)
      .machineState(buildMachineState(states))
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());

    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("suspend_ended")
              .expectStateChanged(0)
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }
}
