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
import static org.qdrin.qfsm.Helper.buildMachineState;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import static org.qdrin.qfsm.TestBundleEquals.testBundleEqualTo;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceEndedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  public static Stream<Arguments> testSuccessTrial() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", new ArrayList<>()),
      Arguments.of("simpleOffer1", "simple1-price-active", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2", "component3"))
    );
  }
  @ParameterizedTest
  @MethodSource
  public void testSuccessTrial(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime tstart = t0.minusDays(30);
    String usageState = priceId.contains("trial") ? "ActiveTrial" : "Active";
    String status = usageState.equals("ActiveTrial") ? "ACTIVE_TRIAL" : "ACTIVE";
    String [] initialStates = {usageState, "Paid", "PriceActive"};
    String[] expectedStates = {usageState, "WaitingPayment", "PriceChanging"};
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(initialStates))
      .productStartDate(tstart)
      .priceNextPayDate(t0.plus(getPriceEndedBefore()))
      .pricePeriod(1)
      .tarificationPeriod(1)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .machineState(expectedMachineState)
      .build();
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.WAITING_PAY_ENDED)
      .wakeAt(t0.plus(getWaitingPayInterval()))
      .build());
      expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.CHANGE_PRICE)
      .build());

    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(initialStates))
              .and()
          .step()
              .sendEvent("price_ended")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(testBundleEqualTo(expectedBundle))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  public static Stream<Arguments> testRejectedNotPriceActive() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", "PriceChanging", new ArrayList<>()),
      Arguments.of("simpleOffer1", "simple1-price-active", "PriceChanged", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "PriceWaiting", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("bundleOffer1", "bundle1-price-active", "PriceWaiting", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "PriceWaiting", Arrays.asList("component1", "component2", "component3"))
    );
  }
  @ParameterizedTest
  @MethodSource
  public void testRejectedNotPriceActive(String offerId, String priceId, String priceState, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime tstart = t0.minusDays(30);
    String usageState = priceId.contains("trial") ? "ActiveTrial" : "Active";
    String status = usageState.equals("ActiveTrial") ? "ACTIVE_TRIAL" : "ACTIVE";
    String [] initialStates = {usageState, "Paid", priceState};
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(initialStates))
      .productStartDate(tstart)
      .priceNextPayDate(t0.plus(getPriceEndedBefore()))
      .pricePeriod(1)
      .tarificationPeriod(1)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());

    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(initialStates))
              .and()
          .step()
              .sendEvent("price_ended")
              .expectEventNotAccepted(19)
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }
}