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
public class ProlongCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static Stream<Arguments> testSuccess() {
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
  public void testSuccess(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    List<String> states = Arrays.asList("Prolongation", "Paid", "PriceActive");
    JsonNode machineState = buildMachineState(states);
    String usage = priceId.contains("trial") ? "ActiveTrial" : "Active";
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
    List<String> expectedStates = Arrays.asList(usage, "Paid", "PriceActive");
    JsonNode expectedMachineState = buildMachineState(expectedStates);

    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("FAKE_STATUS")
      .productStartDate(t0.minusDays(30))
      .tarificationPeriod(1)
      .priceNextPayDate(t0.plus(getPriceEndedBefore()))
      .pricePeriod(1)
      .machineState(machineState)
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(status)
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
          .sendEvent("prolong_completed")
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
