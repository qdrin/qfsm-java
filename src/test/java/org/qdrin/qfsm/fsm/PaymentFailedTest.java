package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;

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
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PaymentFailedTest extends SpringStarter {

  StateMachine<String, String> machine = null;

  private static OffsetDateTime nextPayDate = OffsetDateTime.now().plusDays(30);
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
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
    expectedStates = Arrays.asList("Suspending", "NotPaid", expectedStates.get(2));
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
      .tarificationPeriod(0)
      .pricePeriod(0)
      .machineState(Helper.buildMachineState(expectedStates))
      .build();
    machine = createMachine(bundle);
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    log.debug("actions: {}", variables.get("actions"));

    List<ActionSuite> expectedActions = Arrays.asList(ActionSuite.SUSPEND_EXTERNAL);
    List<ActionSuite> expectedDeleteActions = Arrays.asList(ActionSuite.WAITING_PAY_ENDED, ActionSuite.PRICE_ENDED);

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
              .expectVariable("deleteActions", expectedDeleteActions)
              .expectVariable("actions", expectedActions)
              .and()
          .build();
    plan.test();
    log.debug("actions: {}", variables.get("actions"));
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }
}
