package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.access.StateMachineAccessor;

import static org.junit.Assert.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.ActionSuite;
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

  // @Test
  // public void testDeferredAtPendingActivateSequence() throws Exception {
  //   String offerId = "simpleOffer1";
  //   String priceId = "simple1-price-active";
  //   OffsetDateTime t0 = OffsetDateTime.now();
  //   JsonNode machineState = Helper.buildMachineState("PendingActivate");
  //   TestBundle bundle = new BundleBuilder(offerId, priceId)
  //     .status("PENDING_ACTIVATE")
  //     .productStartDate(t0)
  //     .tarificationPeriod(0)
  //     .priceNextPayDate(nextPayDate)
  //     .pricePeriod(1)
  //     .machineState(machineState)
  //     .build();
  //   StateMachine<String, String> machine = createMachine(bundle);
  //   StateMachineTestPlan<String, String> plan =
  //       StateMachineTestPlanBuilder.<String, String>builder()
  //         .defaultAwaitTime(2)
  //         .stateMachine(machine)
  //         .step()
  //             .expectState("PendingActivate")
  //             .and()
  //         .step()
  //             .sendEvent("payment_processed")  // send payment_processed first
  //             .expectState("PendingActivate")
  //             .and()
  //         .step()
  //             .sendEvent("activation_completed")
  //             // deferred payment_processed cause "Paid" instead of "WaitingPayment"
  //             .expectStates(Helper.stateSuite("Active", "Paid", "PriceActive"))
  //             .and()
  //         .build();
  //   plan.test();
  //   log.debug("machine: {}", machine.getExtendedState());
  //   releaseMachine(machine.getId());
  // }

  private static Stream<Arguments> testDeferredAtPendingActivate() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active"),
      Arguments.of("bundleOffer1", "bundle1-price-active"),
      Arguments.of("customBundleOffer1", "custom1-price-active")
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testDeferredAtPendingActivate(String offerId, String priceId) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = Helper.buildMachineState("PendingActivate");
    TestBundle bundle = new BundleBuilder(offerId, priceId)
      .status("PENDING_ACTIVATE")
      .productStartDate(t0)
      .tarificationPeriod(0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(1)
      .machineState(machineState)
      .build();
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
        Arrays.asList("ActiveTrial", "Paid", "PriceActive")),
      Arguments.of("bundleOffer1", "bundle1-price-trial", 
        Arrays.asList("ActiveTrial", "Paid", "PriceActive")),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFirstTrialPrice(String offerId, String priceId, List<String> states) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = Helper.buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates.set(1, "Paid");

    log.debug("expectedStates: {}", expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId)
      .status("ACTIVE_TRIAL")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(1)
      .machineState(machineState)
      .build();

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
        Arrays.asList("Active", "WaitingPayment", "PriceChanging")),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged")),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanging")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceChanged")),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "WaitingPayment", "PriceActive")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanging")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceChanged")),
      Arguments.of("customBundleOffer1", "custom1-price-active",
        Arrays.asList("Active", "WaitingPayment", "PriceActive"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testFirstActivePrice(String offerId, String priceId, List<String> states) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    JsonNode machineState = Helper.buildMachineState(states);
    List<String> expectedStates = new ArrayList<>(states);
    expectedStates.set(1, "Paid");
    int pricePeriod = states.contains("PriceActive") ? 1 : 0;

    log.debug("expectedStates: {}", expectedStates);
    TestBundle bundle = new BundleBuilder(offerId, priceId)
      .status("ACTIVE")
      .productStartDate(t0)
      .tarificationPeriod(0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(pricePeriod)
      .machineState(machineState)
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(1)
      .pricePeriod(pricePeriod)
      .machineState(Helper.buildMachineState(expectedStates))
      .build();
    machine = createMachine(bundle);
    
    List<ActionSuite> expectedActions = new ArrayList<>();
    List<ActionSuite> expectedDeleteActions = Arrays.asList(ActionSuite.WAITING_PAY_ENDED);

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
              .expectVariable("deleteActions", expectedDeleteActions)
              .expectVariable("actions", expectedActions)
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }
}
