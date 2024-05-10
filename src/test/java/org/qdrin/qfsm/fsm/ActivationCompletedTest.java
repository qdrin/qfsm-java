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
import org.junit.jupiter.params.provider.ValueSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.TestExpected;
import org.qdrin.qfsm.TestExpected.TestExpectedBuilder;
import org.qdrin.qfsm.TestSetup;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;

  private static OffsetDateTime nextPayDate = OffsetDateTime.now().plusDays(30);
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static Stream<Arguments> testSuccess() {
    List<TestSetup> copy = Helper.getTestSetups();
    List<Arguments> args = new ArrayList<>();
    for(TestSetup setup: copy) {
      TestExpectedBuilder builder = TestExpected.builder().pricePeriod(1);
      if(setup.getPriceId().contains("trial")) {
        builder.status("ACTIVE_TRIAL")
          .tarificationPeriod(1)
          .states(Arrays.asList("ActiveTrial", "Paid", "PriceActive"))
          .actions(Arrays.asList(ActionSuite.PRICE_ENDED))
          .nextPayDate(nextPayDate)
          .deleteActions(Arrays.asList());
      }
      else {
        builder.status("ACTIVE")
          .tarificationPeriod(0)
          .states(Arrays.asList("Active", "WaitingPayment", "PriceActive"))
          .actions(Arrays.asList(ActionSuite.WAITING_PAY_ENDED, ActionSuite.PRICE_ENDED))
          .nextPayDate(null)
          .deleteActions(Arrays.asList());
      }
      args.add(Arguments.of(setup, builder.build()));
    }
    return args.stream();
  }
  @ParameterizedTest
  @MethodSource
  public void testSuccess(TestSetup setup, TestExpected exp) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime expectedActiveEndDate = exp.getNextPayDate();
    String offerId = setup.getOfferId();
    String priceId = setup.getPriceId();
    ProductClass driveClass = setup.getProductClass();
    ProductClass componentClass = Helper.getComponentClass(driveClass);
    String[] expectedStates = exp.getStates().toArray(new String[0]);
    int expectedTarificationPeriod = exp.getTarificationPeriod();
    int expectedPricePeriod = exp.getPricePeriod();

    TestBundle bundle = new BundleBuilder(offerId, priceId)
      .productStartDate(t0)
      .priceNextPayDate(nextPayDate)
      .pricePeriod(0)
      .build();
    String expectedStatus = exp.getStatus();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(expectedStatus)
      .tarificationPeriod(expectedTarificationPeriod)
      .pricePeriod(expectedPricePeriod)
      .trialEndDate(expectedActiveEndDate)
      .activeEndDate(expectedActiveEndDate)
      .driveClass(driveClass)
      .componentClass(componentClass)
      .build();
    machine = createMachine(bundle);
    
    List<ActionSuite> expectedActions = exp.getActions();
    List<ActionSuite> expectedDeleteActions = exp.getDeleteActions();

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .step()
              .sendEvent("activation_started")
              .expectState("PendingActivate")
              .expectStateChanged(1)
              .and()
          .step()
              .sendEvent("activation_completed")
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

  private static Stream<Arguments> testSuccessNoNextPayDate() {
    List<TestSetup> copy = Helper.getTestSetups();
    List<Arguments> args = new ArrayList<>();
    List<String> newStates = Arrays.asList("Active", "WaitingPayment", "PriceWaiting");
    JsonNode newMachineState = Helper.buildMachineState(newStates);
    for(TestSetup setup: copy) {
      TestExpectedBuilder builder = TestExpected.builder()
        .pricePeriod(0)
        .status("ACTIVE")
        .tarificationPeriod(0)
        .states(newStates)
        .machineState(newMachineState)
        .actions(Arrays.asList(ActionSuite.WAITING_PAY_ENDED))
        .nextPayDate(null)
        .deleteActions(Arrays.asList());
      args.add(Arguments.of(setup, builder.build()));
    }
    return args.stream();
  }

  @ParameterizedTest
  @MethodSource
  public void testSuccessNoNextPayDate(TestSetup setup, TestExpected exp) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .tarificationPeriod(0)
      .machineState(Helper.buildMachineState("PendingActivate"))
      .build();

    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(exp.getStatus())
      .machineState(exp.getMachineState())
      .tarificationPeriod(0)
      .pricePeriod(0)
      .activeEndDate(null)
      .build();
    
    machine = createMachine(bundle);
    
    OffsetDateTime expectedWaitingPayEnded = t0.plus(getWaitingPayInterval());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("activation_completed")
              .expectStates(Helper.stateSuite(exp.getStates().toArray(new String[0])))
              .expectVariable("deleteActions", exp.getDeleteActions())
              .expectVariable("actions", exp.getActions())
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
    Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
    
    Map<Object, Object> variables = machine.getExtendedState().getVariables(); 
    List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
    OffsetDateTime waitingPayEnded = actions.get(0).getWakeAt();
    assertDates(expectedWaitingPayEnded, waitingPayEnded);
  }

    @Nested
  class CustomBundleComponent {

    private static Stream<Arguments> testCustomBundleComponentActivationCompleted() {
      return Stream.of(
        Arguments.of("customOffer1", "custom1-price-trial",
            "ACTIVE_TRIAL", Arrays.asList("ActiveTrial", "Paid", "PriceActive")),
        Arguments.of("customOffer1", "custom1-price-active",
            "ACTIVE", Arrays.asList("Active", "Paid", "PriceActive"))
      );  
    }

    @ParameterizedTest
    @MethodSource
    // (strings = {"custom1-price-trial", "custom1-price-active"})

    public void testCustomBundleComponentActivationCompleted(String offerId, String priceId,
        String status, List<String> states) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      JsonNode machineState = Helper.buildMachineState(states.toArray(new String[0]));
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", priceId,
        "component1", "component2")
        .tarificationPeriod(2)
        .productStartDate(t0)
        .status(status)
        .machineState(machineState)
        .build();

      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .addBundle(preBundle.bundle)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      // TODO: Добавить анализ на состав бандла
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .machineState(null)  // means that leg is merged to bundle
        .tarificationPeriod(2)
        .status(status)
        .build();

      machine = createMachine(bundle);
      assertEquals(bundle.bundle.getProductId(), preBundle.bundle.getProductId());
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("PendingActivate")
                .and()
            .step()
                .sendEvent("activation_completed")
                .expectStates(Helper.stateSuite(states.get(0), "PaymentFinal", "PriceFinal"))
                // .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      log.debug("states: {}", machine.getState().getIds());
      assertEquals(status, product.getStatus());
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.bundle, bundle.bundle);
      Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
    }

    private static Stream<Arguments> testCustomBundleComponentActivationRejected() {
      return Stream.of(
        Arguments.of("customOffer1", "custom1-price-trial",
            "ACTIVE_TRIAL", Arrays.asList("Suspending", "NotPaid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-trial",
            "SUSPENDED", Arrays.asList("Suspended", "NotPaid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-trial",
            "SUSPENDED", Arrays.asList("Resuming", "Paid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-trial",
            "ACTIVE_TRIAL", Arrays.asList("Prolongation", "Paid", "PriceActive")),
        Arguments.of("customOffer1", "custom1-price-active",
            "ACTIVE", Arrays.asList("Suspending", "NotPaid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-active",
            "SUSPENDED", Arrays.asList("Suspended", "NotPaid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-active",
            "SUSPENDED", Arrays.asList("Resuming", "Paid", "PriceChanging")),
        Arguments.of("customOffer1", "custom1-price-active",
            "ACTIVE", Arrays.asList("Prolongation", "Paid", "PriceActive"))
      );  
    }

    @ParameterizedTest
    @MethodSource

    public void testCustomBundleComponentActivationRejected(String offerId, String priceId,
        String status, List<String> states) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      JsonNode machineState = Helper.buildMachineState(states.toArray(new String[0]));
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", priceId,
        "component1", "component2")
        .tarificationPeriod(2)
        .productStartDate(t0)
        .status(status)
        .machineState(machineState)
        .build();

      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .addBundle(preBundle.bundle)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .status("PENDING_ACTIVATE")
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .status("PENDING_ACTIVATE")
        .build();

      machine = createMachine(bundle);
      assertEquals(bundle.bundle.getProductId(), preBundle.bundle.getProductId());
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("PendingActivate")
                .and()
            .step()
                .sendEvent("activation_completed")
                .expectStates("PendingActivate")
                .expectStateChanged(0)
                .expectEventNotAccepted(0)
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      log.debug("states: {}", machine.getState().getIds());
      assertEquals("PENDING_ACTIVATE", product.getStatus());
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.bundle, bundle.bundle);
      Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
    }
  }
}
