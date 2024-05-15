package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;

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
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import static org.qdrin.qfsm.TestBundleEquals.testBundleEqualTo;
import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.service.QStateMachineContextConverter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  public static Stream<Arguments> testSuccessTrial() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", Arrays.asList("component1", "component2", "component3"))
    );
  }
  @ParameterizedTest
  @MethodSource
  public void testSuccessTrial(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    String[] expectedStates = Helper.stateSuite("ActiveTrial", "Paid", "PriceActive");
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("PENDING_ACTIVATE")
      .machineState(buildMachineState("PendingActivate"))
      .productStartDate(t0)
      .priceNextPayDate(t1)
      .pricePeriod(0)
      .tarificationPeriod(0)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("ACTIVE_TRIAL")
      .tarificationPeriod(1)
      .pricePeriod(1)
      .trialEndDate(t1)
      .activeEndDate(t1)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.PRICE_ENDED)
      .wakeAt(t1.minus(getPriceEndedBefore()))
      .build()
    );

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("PendingActivate")
              .and()
          .step()
              .sendEvent("activation_completed")
              .expectStates(expectedStates)
              .expectVariableWith(testBundleEqualTo(expectedBundle))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  public static Stream<Arguments> testSuccessActive() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-active", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-active", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-active", Arrays.asList("component1", "component2", "component3"))
    );
  }
  @ParameterizedTest
  @MethodSource
  public void testSuccessActive(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    OffsetDateTime waitPaymentTime = t0.plus(getWaitingPayInterval());
    String[] expectedStates = Helper.stateSuite("Active", "WaitingPayment", "PriceActive");

    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status("PENDING_ACTIVATE")
      .machineState(buildMachineState("PendingActivate"))
      .productStartDate(t0)
      .priceNextPayDate(t1)
      .pricePeriod(0)
      .tarificationPeriod(0)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("ACTIVE")
      .tarificationPeriod(0)
      .pricePeriod(1)
      .activeEndDate(t1)
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(machine.getId());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).wakeAt(waitPaymentTime).build());
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).wakeAt(t1.minus(getPriceEndedBefore())).build());


    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("PendingActivate")
              .and()
          .step()
              .sendEvent("activation_completed")
              .expectStates(expectedStates)
              .expectVariableWith(testBundleEqualTo(expectedBundle))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
  }

  public static Stream<Arguments> testSuccessNoNextPayDate() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", "ACTIVE_TRIAL", new ArrayList<>()),
      Arguments.of("simpleOffer1", "simple1-price-active", "ACTIVE", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "ACTIVE_TRIAL", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("bundleOffer1", "bundle1-price-active", "ACTIVE", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "ACTIVE_TRIAL", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "ACTIVE", Arrays.asList("component1", "component2", "component3"))
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testSuccessNoNextPayDate(
        String offerId, String priceId, String expectedStatus, List<String> componentOfferIds) throws Exception {

    OffsetDateTime t0 = OffsetDateTime.now();
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .tarificationPeriod(0)
      .status("PENDING_ACTIVATE")
      .machineState(buildMachineState("PendingActivate"))
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    String productId = bundle.drive.getProductId();
    String expectedUsage = expectedStatus.equals("ACTIVE") ? "Active" : "ActiveTrial";
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(expectedStatus)
      .tarificationPeriod(0)
      .pricePeriod(0)
      .activeEndDate(null)
      .build();
    machine = createMachine(bundle);
    
    OffsetDateTime taskDate = t0.plus(getWaitingPayInterval());
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).wakeAt(taskDate).build());

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("activation_completed")
              .expectStates(Helper.stateSuite(expectedUsage, "WaitingPayment", "PriceWaiting"))
              .expectVariableWith(testBundleEqualTo(expectedBundle))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  @Nested
  class CustomBundle {
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
    public void testCustomBundleComponentActivationCompleted(String offerId, String priceId,
        String status, List<String> states) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      JsonNode machineState = buildMachineState(states.toArray(new String[0]));
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
        .isIndependent(true)
        .addBundle(preBundle.bundle)
        .machineState(buildMachineState("PendingActivate"))
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      // TODO: Добавить анализ на состав бандла
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .isIndependent(false)  // means leg becomes merged to bundle
        .tarificationPeriod(2)
        .status(status)
        .build();

      TaskPlan expectedTasks = new TaskPlan(product.getProductId());
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
                .expectVariableWith(testBundleEqualTo(expectedBundle))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
    }

    private static Stream<Arguments> testCustomBundleComponentActivationRejected() {
      return Stream.of(
        Arguments.of("customOffer1", "custom1-price-trial",
            "PendingActivate", Arrays.asList("PendingActivate")),
        Arguments.of("customOffer1", "custom1-price-active",
            "PendingActivate", Arrays.asList("PendingActivate")),
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
      JsonNode machineState = buildMachineState(states.toArray(new String[0]));
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
        .machineState(buildMachineState("PendingActivate"))
        .status("PENDING_ACTIVATE")
        .isIndependent(true)
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .isIndependent(true)
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

    public static Stream<Arguments> testBundleActivationWithCustomComponentIndependent() {
      return Stream.of(
        Arguments.of("custom1-price-trial", Arrays.asList("component1", "component2", "component3"), "component3"),
        Arguments.of("custom1-price-active", Arrays.asList("component1", "component2", "component3"), "component3")
      );
    }

    @ParameterizedTest
    @MethodSource
    public void testBundleActivationWithCustomComponentIndependent(String priceId, List<String> componentOfferIds, String independentOfferId) throws Exception {
      String offerId = "customBundleOffer1";
      OffsetDateTime t0 = OffsetDateTime.now().minusSeconds(30);
      OffsetDateTime t1 = t0.plusDays(30);
      OffsetDateTime waitPaymentTime = t0.plus(getWaitingPayInterval());
  
      TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .status("PENDING_ACTIVATE")
        .machineState(buildMachineState("PendingActivate"))
        .productStartDate(t0)
        .priceNextPayDate(t1)
        .pricePeriod(0)
        .tarificationPeriod(0)
        .build();
      assertEquals(componentOfferIds.size(), bundle.components().size());
      String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
      Product independent = bundle.getByOfferId(independentOfferId);
      independent.getMachineContext().setIsIndependent(true);
      independent.setProductStartDate(t0.plusSeconds(30));
      independent.getMachineContext().setMachineState(buildMachineState("PendingActivate"));
  
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .status(status)
        .pricePeriod(1)
        .tarificationPeriod(-1)
        .activeEndDate(t1)
        .build();
      independent = expectedBundle.getByOfferId(independentOfferId);
      // Leg is independent yet and its parameters must stay unchanged
      independent.setActiveEndDate(null);
      independent.setStatus("PENDING_ACTIVATE");
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
                .expectVariableWith(testBundleEqualTo(expectedBundle))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertEquals(null, bundle.getByOfferId(independentOfferId).getActiveEndDate());
      log.debug("states: {}", machine.getState().getIds());
    }
  }
}
