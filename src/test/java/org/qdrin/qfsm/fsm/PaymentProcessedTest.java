package org.qdrin.qfsm.fsm;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;

import static org.junit.Assert.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.Helper.buildMachineState;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PaymentProcessedTest extends SpringStarter {

  StateMachine<String, String> machine = null;

  private static OffsetDateTime nextPayDate = OffsetDateTime.now().plusDays(30);

  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static Stream<Arguments> testDeferredAtPendingActivate() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testDeferredAtPendingActivate(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState("PendingActivate");
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("PENDING_ACTIVATE")
      .productStartDate(t0)
      .tarificationPeriod(0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    StateMachine<String, String> machine = createMachine(bundle);
    Mono<Message<String>> msg = Mono.just((MessageBuilder.withPayload("payment_processed").build()));
    StateMachineEventResult<String, String> res = machine.sendEvent(msg).blockLast();
    log.debug("res: {}", res);
    assertEquals(StateMachineEventResult.ResultType.DEFERRED, res.getResultType());
    Message<String> deferred = res.getMessage();
    releaseMachine(machine.getId());
    machine = createMachine(bundle);
    StateMachineTestPlan<String, String> plan =
    StateMachineTestPlanBuilder.<String, String>builder()
      .defaultAwaitTime(2)
      .stateMachine(machine)
      .step()
          .expectState("PendingActivate")
          .and()
      .step()
          .sendEvent("activation_completed")
          .expectStates(Helper.stateSuite("Active", "WaitingPayment", "PriceActive"))
          .and()
      .step()
          .sendEvent(deferred)
          .expectStates(Helper.stateSuite("Active", "Paid", "PriceActive"))
          .and()
      .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  private static Stream<Arguments> testFirstTrialPrice() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", 
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFirstTrialPrice(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates.set(1, "Paid");

    log.debug("expectedStates: {}", expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("ACTIVE_TRIAL")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(1)
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_processed")
              .expectStateChanged(0)
              .expectEventNotAccepted(19)
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testFirstActivePrice() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFirstActivePrice(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates.set(1, "Paid");
    int pricePeriod = states.contains("PriceActive") ? 1 : 0;

    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("ACTIVE")
      .productStartDate(t0)
      .tarificationPeriod(0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(pricePeriod)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(1)
      .pricePeriod(pricePeriod)
      .machineState(expectedMachineState)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_processed")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testWithNoActivePrice() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testWithNoActivePrice(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates.set(1, "Paid");
    int pricePeriod = 1;

    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("ACTIVE")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(pricePeriod)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(2)
      .pricePeriod(pricePeriod)
      .machineState(expectedMachineState)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_processed")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testWithActivePrice() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", 
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testWithActivePrice(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList("Prolongation", "Paid", "PriceActive");
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";

    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .productStartDate(t0.minusDays(30))
      .tarificationPeriod(1)
      .priceNextPayDate(t1)
      .pricePeriod(2)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(2)
      .machineState(expectedMachineState)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.PROLONG_EXTERNAL).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_processed")
              .expectStateEntered("Paid", "Prolongation")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testFromSuspended() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFromSuspended(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList("Suspended", "Paid", "PriceChanging");

    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("SUSPENDED")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .priceNextPayDate(t0.minusDays(20))
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(2)
      .machineState(expectedMachineState)
      .pricePeriod(0)  // Resetting pricePeriod when resume
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_processed")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testFromSuspendedWithEndDate() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanging"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceChanged"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceActive"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFromSuspendedWithEndDate(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList("Suspended", "Paid", "PriceChanging");

    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("SUSPENDED")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .priceNextPayDate(t0.minusDays(20))
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(2)
      .machineState(expectedMachineState)
      .pricePeriod(0)
      .build();
    machine = createMachine(bundle);
    
    EventProperties eventPropeties = EventProperties.builder().endDate(t1).build();
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).variables(Map.of("eventProperties", eventPropeties)) .build());

    Message<String> message = MessageBuilder.withPayload("payment_processed").setHeader("eventProperties", eventPropeties).build();
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent(message)
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testPriceWaitingSuccess() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceWaiting"), Arrays.asList()),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceWaiting"), Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", 
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceWaiting"), Arrays.asList("component1", "component2")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceWaiting"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "WaitingPayment", "PriceWaiting"), Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceWaiting"), Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testPriceWaitingSuccess(String offerId, String priceId, List<String> states, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList(states.get(0), "Paid", "PriceChanging");
    expectedStates.set(1, "Paid");
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";


    log.debug("expectedStates: {}", expectedStates);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .productStartDate(t0)
      .tarificationPeriod(1)
      .activeEndDate(t0.plus(getPriceEndedBefore()))
      .priceNextPayDate(t0.plus(getPriceEndedBefore()))
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(2)
      .machineState(expectedMachineState)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
    EventProperties eventProperties = EventProperties.builder().endDate(t1).build();
    expectedTasks.getCreatePlan().get(0).getVariables().put("eventProperties", eventProperties);
    Message<String> msg = MessageBuilder
        .withPayload("payment_processed")
        .setHeader("eventProperties", eventProperties)
        .build();
    log.debug("sending message: {}", msg);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent(msg)
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());

  }

  @Nested
  class CustomBundle {
    private static Stream<Arguments> testNoActivePriceWithPendingActivateComponent() {
      return Stream.of(
        Arguments.of("customBundleOffer1", "custom1-price-active",
          Arrays.asList("Active", "WaitingPayment", "PriceChanging")),
        Arguments.of("customBundleOffer1", "custom1-price-active",
          Arrays.asList("Active", "WaitingPayment", "PriceChanged")),
        Arguments.of("customBundleOffer1", "custom1-price-trial",
          Arrays.asList("ActiveTrial", "WaitingPayment", "PriceChanging")),
        Arguments.of("customBundleOffer1", "custom1-price-trial",
          Arrays.asList("ActiveTrial", "WaitingPayment", "PriceChanged"))
      );
    }
    @ParameterizedTest
    @MethodSource
    public void testNoActivePriceWithPendingActivateComponent(String offerId, String priceId, List<String> states) throws Exception {
      List<String> componentOfferIds = Arrays.asList("component1", "component2", "component3");
      OffsetDateTime t0 = OffsetDateTime.now();
      JsonNode machineState = buildMachineState(states);
      List<String> expectedStates = new ArrayList<>(states);
      String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
  
      expectedStates.set(1, "Paid");
      int pricePeriod = 1;
  
      log.debug("expectedStates: {}", expectedStates);
      JsonNode expectedMachineState = buildMachineState(expectedStates);
      TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .status(status)
        .productStartDate(t0)
        .tarificationPeriod(1)
        .priceNextPayDate(nextPayDate)
        .pricePeriod(pricePeriod)
        .machineState(machineState)
        .unmergeComponent("component3", "PENDING_ACTIVATE", Arrays.asList("PendingActivate"))
        .build();
      assertEquals(componentOfferIds.size(), bundle.components().size());
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .tarificationPeriod(2)
        .pricePeriod(pricePeriod)
        .machineState(expectedMachineState)
        .unmergeComponent("component3", "PENDING_ACTIVATE", Arrays.asList("PendingActivate"))
        .build();
      machine = createMachine(bundle);
      
      TaskPlan expectedTasks = new TaskPlan(machine.getId());
      expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
  
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(Helper.stateSuite(states))
                .and()
            .step()
                .sendEvent("payment_processed")
                .expectStates(Helper.stateSuite(expectedStates))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertProductEquals(expectedBundle.drive, bundle.drive);
      assertProductEquals(expectedBundle.components(), bundle.components());
      Product component3 = bundle.getByOfferId("component3");
      assertEquals("PENDING_ACTIVATE", component3.getStatus());
      assertEquals("PendingActivate", component3.getMachineContext().getMachineState().asText());
    }
    
    private static Stream<Arguments> testActivePriceWithPendingActivateComponent() {
      return Stream.of(
        Arguments.of("customBundleOffer1", "custom1-price-active",
          Arrays.asList("Active", "WaitingPayment", "PriceActive")),
        Arguments.of("customBundleOffer1", "custom1-price-trial",
          Arrays.asList("ActiveTrial", "WaitingPayment", "PriceActive"))
      );
    }
    @ParameterizedTest
    @MethodSource
    public void testActivePriceWithPendingActivateComponent(String offerId, String priceId, List<String> states) throws Exception {
      List<String> componentOfferIds = Arrays.asList("component1", "component2", "component3");
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      JsonNode machineState = buildMachineState(states);
      String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
  
      List<String>  expectedStates = Arrays.asList("Prolongation", "Paid", "PriceActive");
      int pricePeriod = 1;
  
      log.debug("expectedStates: {}", expectedStates);
      JsonNode expectedMachineState = buildMachineState(expectedStates);
      TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .status(status)
        .productStartDate(t0.minusDays(30))
        .tarificationPeriod(1)
        .priceNextPayDate(t1)
        .pricePeriod(2)
        .machineState(machineState)
        .unmergeComponent("component3", "PENDING_ACTIVATE", Arrays.asList("PendingActivate"))
        .build();
      assertEquals(componentOfferIds.size(), bundle.components().size());
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .tarificationPeriod(2)
        .pricePeriod(2)
        .machineState(expectedMachineState)
        .unmergeComponent("component3", "PENDING_ACTIVATE", Arrays.asList("PendingActivate"))
        .build();
      machine = createMachine(bundle);
      
      TaskPlan expectedTasks = new TaskPlan(machine.getId());
      expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
      expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.PROLONG_EXTERNAL).build());
  
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(Helper.stateSuite(states))
                .and()
            .step()
                .sendEvent("payment_processed")
                .expectStates(Helper.stateSuite(expectedStates))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertProductEquals(expectedBundle.drive, bundle.drive);
      assertProductEquals(expectedBundle.components(), bundle.components());
      Product component3 = bundle.getByOfferId("component3");
      assertEquals("PENDING_ACTIVATE", component3.getStatus());
      assertEquals("PendingActivate", component3.getMachineContext().getMachineState().asText());
    }

    private static Stream<Arguments> testPriceWaitingWithPendingActivateComponent() {
      return Stream.of(
        Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("ActiveTrial", "WaitingPayment", "PriceWaiting")),
        Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("Active", "WaitingPayment", "PriceWaiting"))
      );  
    }
    @ParameterizedTest
    @MethodSource
    public void testPriceWaitingWithPendingActivateComponent(String offerId, String priceId, List<String> states) throws Exception {
      List<String> componentOfferIds = Arrays.asList("component1", "component2", "component3");
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(30);
      JsonNode machineState = buildMachineState(states);
      List<String> expectedStates = Arrays.asList(states.get(0), "Paid", "PriceChanging");
      expectedStates.set(1, "Paid");
      String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
  
  
      log.debug("expectedStates: {}", expectedStates);
      JsonNode expectedMachineState = buildMachineState(expectedStates);
      TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .status(status)
        .productStartDate(t0)
        .tarificationPeriod(1)
        .activeEndDate(t0.plus(getPriceEndedBefore()))
        .priceNextPayDate(t0.plus(getPriceEndedBefore()))
        .pricePeriod(1)
        .machineState(machineState)
        .unmergeComponent("component3", "PENDING_ACTIVATE", Arrays.asList("PendingActivate"))
        .build();
      assertEquals(componentOfferIds.size(), bundle.components().size());
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .tarificationPeriod(2)
        .machineState(expectedMachineState)
        .build();
      machine = createMachine(bundle);
      
      TaskPlan expectedTasks = new TaskPlan(machine.getId());
      expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
      expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
      EventProperties eventProperties = EventProperties.builder().endDate(t1).build();
      expectedTasks.getCreatePlan().get(0).getVariables().put("eventProperties", eventProperties);
      Message<String> msg = MessageBuilder
          .withPayload("payment_processed")
          .setHeader("eventProperties", eventProperties)
          .build();
      log.debug("sending message: {}", msg);
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(Helper.stateSuite(states))
                .and()
            .step()
                .sendEvent(msg)
                .expectStates(Helper.stateSuite(expectedStates))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertProductEquals(expectedBundle.drive, bundle.drive);
      assertProductEquals(expectedBundle.components(), bundle.components());
      Product component3 = bundle.getByOfferId("component3");
      assertEquals("PENDING_ACTIVATE", component3.getStatus());
      assertEquals("PendingActivate", component3.getMachineContext().getMachineState().asText());
    }
  }
}
