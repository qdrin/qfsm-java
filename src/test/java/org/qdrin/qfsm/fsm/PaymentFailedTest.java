package org.qdrin.qfsm.fsm;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PaymentFailedTest extends SpringStarter {

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
    Mono<Message<String>> msg = Mono.just((MessageBuilder.withPayload("payment_failed").build()));
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
          .expectStates(Helper.stateSuite("Suspending", "NotPaid", "PriceActive"))
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

    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("ACTIVE_TRIAL")
      .productStartDate(t0)
      .tarificationPeriod(0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(1)
      .machineState(machineState)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
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
              .sendEvent("payment_failed")
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
  public void testFirstActivePrice(String offerId, String priceId, List<String> states, List<String> componentOfferIds)
             throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates = Arrays.asList("Suspending", "NotPaid", expectedStates.get(2));
    int pricePeriod = states.contains("PriceActive") ? 1 : 0;

    log.debug("expectedStates: {}", expectedStates);
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
      .tarificationPeriod(0)
      .pricePeriod(pricePeriod)
      .machineState(buildMachineState(expectedStates))
      .build();
    machine = createMachine(bundle);
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    log.debug("actions: {}", variables.get("actions"));

    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.SUSPEND_EXTERNAL).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_failed")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testCommon() {
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
  public void testCommon(String offerId, String priceId, List<String> states, List<String> componentOfferIds)
             throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates = Arrays.asList("Suspending", "NotPaid", expectedStates.get(2));
    int pricePeriod = 1;

    log.debug("expectedStates: {}", expectedStates);
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
      .machineState(buildMachineState(expectedStates))
      .build();
    machine = createMachine(bundle);
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    log.debug("actions: {}", variables.get("actions"));

    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.SUSPEND_EXTERNAL).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent("payment_failed")
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }
}
