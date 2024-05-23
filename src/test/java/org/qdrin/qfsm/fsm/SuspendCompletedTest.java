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


@Slf4j
public class SuspendCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;

  private static OffsetDateTime nextPayDate = OffsetDateTime.now().plusDays(30);
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static Stream<Arguments> testSuccess() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", "PriceChanging", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "PriceChanging", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "PriceChanging", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-active", "PriceChanging", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", "PriceChanging", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "PriceChanging", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-trial", "PriceChanged", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "PriceChanged", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "PriceChanged", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-active", "PriceChanged", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", "PriceChanged", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "PriceChanged", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-trial", "PriceActive", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "PriceActive", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "PriceActive", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-active", "PriceActive", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", "PriceActive", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "PriceActive", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-trial", "PriceWaiting", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "PriceWaiting", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "PriceWaiting", Arrays.asList("component1", "component2")),
      Arguments.of("simpleOffer1", "simple1-price-active", "PriceWaiting", Arrays.asList()),
      Arguments.of("bundleOffer1", "bundle1-price-active", "PriceWaiting", Arrays.asList("component1", "component2")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "PriceWaiting", Arrays.asList("component1", "component2"))
    );  
  }
  @ParameterizedTest
  @MethodSource
  public void testSuccess(String offerId, String priceId, String priceState, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    List<String> states = Arrays.asList("Suspending", "NotPaid", priceState);
    List<String> expectedStates = Arrays.asList("Suspended", "NotPaid", priceState);
    JsonNode machineState = buildMachineState(states);
    JsonNode expectedMachineState = buildMachineState(expectedStates);
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";

    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .productStartDate(t0.minusDays(30))
      .tarificationPeriod(1)
      .priceNextPayDate(t0.plus(getPriceEndedBefore()))
      .pricePeriod(1)
      .machineState(machineState)
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("SUSPENDED")
      .machineState(expectedMachineState)
      .build();

    String productId = bundle.drive.getProductId();
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.SUSPEND_ENDED)
      .wakeAt(t0.plus(getSuspendInterval()))  // TODO: Уточнить, как рассчитывается продуктовый suspend_ended_time
      .build()
    );
  
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
          .sendEvent("suspend_completed")
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
