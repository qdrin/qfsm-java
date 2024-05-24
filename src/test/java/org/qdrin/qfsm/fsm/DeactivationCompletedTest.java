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
import org.qdrin.qfsm.ProductClass;

import static org.qdrin.qfsm.Helper.buildMachineState;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class DeactivationCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

    private static Stream<Arguments> testDeferredAtPendingDisconnect() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testDeferredAtPendingDisconnect(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    // List<String> states = Arrays.asList("Disconnect");
    List<String> states = Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal");
    List<String> expectedStates = Arrays.asList("Disconnection", "PaymentFinal", "PriceFinal");
    JsonNode machineState = buildMachineState(states);

    BundleBuilder builder = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("PENDING_DISCONNECT")
      .productStartDate(t0)
      .tarificationPeriod(1)
      .machineState(machineState);
    for(String componentOfferId: componentOfferIds) builder.unmergeComponent(componentOfferId, "PENDING_DISCONNECT", states);
    TestBundle bundle = builder.build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    StateMachine<String, String> machine = createMachine(bundle);
    log.debug("machine states: {}", machine.getState().getIds());
    Mono<Message<String>> msg = Mono.just((MessageBuilder.withPayload("deactivation_completed").build()));
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
          .expectStates(Helper.stateSuite(states))
          .and()
      .step()
          .sendEvent("disconnect")
          .expectStates(Helper.stateSuite(states))
          .and()
      .step()
          .sendEvent(deferred)
          .expectStates(Helper.stateSuite(expectedStates))
          .and()
      .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  private static Stream<Arguments> testHead() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-active", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testHead(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    List<String> states = Arrays.asList("Disconnection", "PaymentFinal", "PriceFinal");
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList("Disconnect");
    JsonNode expectedMachineState = buildMachineState(expectedStates);

    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("PENDING_DISCONNECT")
      .productStartDate(t0.minusDays(30))
      .tarificationPeriod(1)
      .machineState(machineState)
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("DISCONNECT")
      .machineState(expectedMachineState)
      .build();

    String productId = bundle.drive.getProductId();
    TaskPlan expectedTasks = new TaskPlan(productId);
  
    assertEquals(componentOfferIds.size(), bundle.components().size());
    StateMachine<String, String> machine = createMachine(bundle);
    StateMachineTestPlan<String, String> plan =
    StateMachineTestPlanBuilder.<String, String>builder()
      .defaultAwaitTime(2)
      .stateMachine(machine)
      .step()
          .expectStates(Helper.stateSuite(states))
          .and()
      .step()
          .sendEvent("deactivation_completed")
          .expectStates(Helper.stateSuite(expectedStates))
          .expectVariableWith(taskPlanEqualTo(expectedTasks))
          .and()
      .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  private static Stream<Arguments> testComponent() {
    return Stream.of(
      Arguments.of("component1", ProductClass.BUNDLE_COMPONENT),
      Arguments.of("component1", ProductClass.CUSTOM_BUNDLE_COMPONENT)
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testComponent(String offerId, ProductClass pclass) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    List<String> states = Arrays.asList("Disconnection", "PaymentFinal", "PriceFinal");
    JsonNode machineState = buildMachineState(states);
    List<String> expectedStates = Arrays.asList("Disconnect");
    JsonNode expectedMachineState = buildMachineState(expectedStates);

    TestBundle bundle = new BundleBuilder(offerId, null)
      .status("PENDING_DISCONNECT")
      .productStartDate(t0.minusDays(30))
      .driveClass(pclass)
      .tarificationPeriod(1)
      .machineState(machineState)
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("DISCONNECT")
      .machineState(expectedMachineState)
      .build();

    String productId = bundle.drive.getProductId();
    TaskPlan expectedTasks = new TaskPlan(productId);

    StateMachine<String, String> machine = createMachine(bundle);
    StateMachineTestPlan<String, String> plan =
    StateMachineTestPlanBuilder.<String, String>builder()
      .defaultAwaitTime(2)
      .stateMachine(machine)
      .step()
          .expectStates(Helper.stateSuite(states))
          .and()
      .step()
          .sendEvent("deactivation_completed")
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
