package org.qdrin.qfsm.fsm;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductClass;

import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import static org.qdrin.qfsm.TestBundleEquals.testBundleEqualTo;
import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.qdrin.qfsm.utils.DisconnectModeCalculator.DisconnectMode;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DeactivationStartedTest extends SpringStarter {

  private static final String productTrialCharName = "TrialDeactivationMode";
  private static final String productActiveCharName = "ActiveDeactivationMode";
  private static final String eventCharName = "deactivationMode";

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static TaskPlan createDefaultTaskPlan(TestBundle bundle, OffsetDateTime activeEndDate) {
    String productId = bundle.drive.getProductId();
    TaskPlan tasks = new TaskPlan(productId);
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.RESUME_EXTERNAL).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.DISCONNECT).build());

    for(Product product: bundle.products) {
      tasks.addToCreatePlan(TaskDef.builder()
        .productId(product.getProductId())
        .type(TaskType.DISCONNECT)
        .wakeAt(activeEndDate)
        .build(), false);
    }

    return tasks;
  }

  public static Stream<Arguments> postponedableData() {
    List<String> components = Arrays.asList("component1", "component2", "component3");
    List<String> empty = new ArrayList<>();

    List<String> trial = Arrays.asList("ActiveTrial", "Paid", "PriceActive");
    List<String> active = Arrays.asList("Active", "Paid", "PriceActive");
    List<String> prolongation = Arrays.asList("Prolongation", "Paid", "PriceActive");
    List<String> resuming = Arrays.asList("Resuming", "Paid", "PriceActive");

    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", trial, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("simpleOffer1", "simple1-price-active", active, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", trial, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-active", active, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", trial, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-active", active, components, DisconnectMode.POSTPONED, null, null),

      Arguments.of("simpleOffer1", "simple1-price-trial", prolongation, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("simpleOffer1", "simple1-price-active", prolongation, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", prolongation, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-active", prolongation, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", prolongation, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-active", prolongation, components, DisconnectMode.POSTPONED, null, null),

      Arguments.of("simpleOffer1", "simple1-price-trial", resuming, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("simpleOffer1", "simple1-price-active", resuming, empty, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", resuming, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("bundleOffer1", "bundle1-price-active", resuming, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", resuming, components, DisconnectMode.POSTPONED, null, null),
      Arguments.of("customBundleOffer1", "custom1-price-active", resuming, components, DisconnectMode.POSTPONED, null, null),
      
      // productCharacteristic = "Immediate"
      Arguments.of("simpleOffer1", "simple1-price-trial", trial, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("simpleOffer1", "simple1-price-active", active, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", trial, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-active", active, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", trial, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-active", active, components, DisconnectMode.IMMEDIATE, "Immediate", null),

      Arguments.of("simpleOffer1", "simple1-price-trial", prolongation, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("simpleOffer1", "simple1-price-active", prolongation, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", prolongation, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-active", prolongation, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", prolongation, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-active", prolongation, components, DisconnectMode.IMMEDIATE, "Immediate", null),

      Arguments.of("simpleOffer1", "simple1-price-trial", resuming, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("simpleOffer1", "simple1-price-active", resuming, empty, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-trial", resuming, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("bundleOffer1", "bundle1-price-active", resuming, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-trial", resuming, components, DisconnectMode.IMMEDIATE, "Immediate", null),
      Arguments.of("customBundleOffer1", "custom1-price-active", resuming, components, DisconnectMode.IMMEDIATE, "Immediate", null),

      // productCharacteristic = "Immediate" and eventCharacteristic = "Postponed"
      Arguments.of("simpleOffer1", "simple1-price-trial", trial, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("simpleOffer1", "simple1-price-active", active, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", trial, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-active", active, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", trial, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-active", active, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),

      Arguments.of("simpleOffer1", "simple1-price-trial", prolongation, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("simpleOffer1", "simple1-price-active", prolongation, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", prolongation, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-active", prolongation, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", prolongation, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-active", prolongation, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),

      Arguments.of("simpleOffer1", "simple1-price-trial", resuming, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("simpleOffer1", "simple1-price-active", resuming, empty, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", resuming, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("bundleOffer1", "bundle1-price-active", resuming, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", resuming, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),
      Arguments.of("customBundleOffer1", "custom1-price-active", resuming, components, DisconnectMode.POSTPONED, "Immediate", "Postponed"),

      // productCharacteristic = "Postponed" and eventCharacteristic = "Immediate"
      Arguments.of("simpleOffer1", "simple1-price-trial", trial, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("simpleOffer1", "simple1-price-active", active, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", trial, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-active", active, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", trial, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-active", active, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),

      Arguments.of("simpleOffer1", "simple1-price-trial", prolongation, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("simpleOffer1", "simple1-price-active", prolongation, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", prolongation, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-active", prolongation, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", prolongation, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-active", prolongation, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),

      Arguments.of("simpleOffer1", "simple1-price-trial", resuming, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("simpleOffer1", "simple1-price-active", resuming, empty, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", resuming, components, DisconnectMode.IMMEDIATE,"Postponed", "Immediate"),
      Arguments.of("bundleOffer1", "bundle1-price-active", resuming, components, DisconnectMode.IMMEDIATE,"Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", resuming, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate"),
      Arguments.of("customBundleOffer1", "custom1-price-active", resuming, components, DisconnectMode.IMMEDIATE, "Postponed", "Immediate")
      );
  }

  @ParameterizedTest
  @MethodSource("postponedableData")
  public void testPostpondable(String offerId, String priceId, List<String> states,
        List<String> componentOfferIds, DisconnectMode expectedMode, String productCharValue, String eventCharValue) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime activeEndDate = t0.plusDays(15);
    OffsetDateTime t1 = expectedMode == DisconnectMode.POSTPONED ? activeEndDate : t0;
    OffsetDateTime tstart = t0.minusDays(30);
    String status = states.get(0).equals("ActiveTrial") ? "ACTIVE_TRIAL" : "ACTIVE";
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(states))
      .productStartDate(tstart)
      .activeEndDate(activeEndDate)
      .pricePeriod(1)
      .tarificationPeriod(2)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());

    if(productCharValue != null) {
      ProductCharacteristic ch = new ProductCharacteristic();
      String productCharName = priceId.contains("trial") ? productTrialCharName : productActiveCharName;
      ch.setRefName(productCharName);
      ch.setValueType("string");
      ch.setValue(productCharValue);
      bundle.drive.getCharacteristic().add(ch);
    }
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("PENDING_DISCONNECT")
      .activeEndDate(t1)
      .build();
    expectedBundle.components().stream().forEach(c -> c.getMachineContext().setIsIndependent(true));
    TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t1);

    List<Characteristic> eventChars = null;
    if(eventCharValue != null) {
      eventChars = new ArrayList<>();
      Characteristic evch = new Characteristic();
      evch.setName(eventCharName);
      evch.setValue(eventCharValue);
      eventChars.add(evch);
    }

    Message<String> message = MessageBuilder
        .withPayload("deactivation_started")
        .setHeader("characteristics", eventChars)
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
              .sendEvent(message)
              .expectStates(Helper.stateSuite("PendingDisconnect", "PaymentFinal", "PriceFinal"))
              .expectVariableWith(testBundleEqualTo(expectedBundle))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  public static Stream<Arguments> testNonActiveStates() {
    List<String> components = Arrays.asList("component1", "component2", "component3");
    List<String> empty = new ArrayList<>();

    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", empty),
      Arguments.of("simpleOffer1", "simple1-price-active",empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", components),
      Arguments.of("bundleOffer1", "bundle1-price-active", components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", components),
      Arguments.of("customBundleOffer1", "custom1-price-active", components)
      );
  }

  @ParameterizedTest
  @MethodSource
  public void testNonActiveStates(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime activeEndDate = t0;
    OffsetDateTime t1 = activeEndDate;
    OffsetDateTime tstart = t0.minusDays(30);
    for(String usage: Arrays.asList("Suspending", "Suspended")) {
      for(String payment: Arrays.asList("WaitingPayment", "NotPaid")) {
        for(String price: Arrays.asList("PriceChanging", "PriceChanged", "PriceWaiting")) {
          List<String> states = Arrays.asList(usage, payment, price);
          TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
            .status("ACTIVE")
            .machineState(buildMachineState(states))
            .productStartDate(tstart)
            .activeEndDate(activeEndDate)
            .pricePeriod(1)
            .tarificationPeriod(2)
            .build();
          assertEquals(componentOfferIds.size(), bundle.components().size());
          String productId = bundle.drive.getProductId();
          TestBundle expectedBundle = new BundleBuilder(bundle)
            .status("PENDING_DISCONNECT")
            .activeEndDate(t1)
            .build();
          expectedBundle.components().stream().forEach(c -> c.getMachineContext().setIsIndependent(true));
          TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t1);
      
          machine = createMachine(bundle);
    
        StateMachineTestPlan<String, String> plan =
            StateMachineTestPlanBuilder.<String, String>builder()
              .defaultAwaitTime(2)
              .stateMachine(machine)
              .step()
                  .expectStates(Helper.stateSuite(states))
                  .and()
              .step()
                  .sendEvent("deactivation_started")
                  .expectStates(Helper.stateSuite("PendingDisconnect", "PaymentFinal", "PriceFinal"))
                  .expectVariableWith(testBundleEqualTo(expectedBundle))
                  .expectVariableWith(taskPlanEqualTo(expectedTasks))
                  .and()
              .build();
        plan.test();
        releaseMachine(machine.getId());
        }
      }
    }
  }

  @Nested
  class CustomBundle {

    private static Stream<Arguments> testCustomBundleComponent() {
      return Stream.of(
        Arguments.of("customOffer1", "custom1-price-trial",
            "ACTIVE_TRIAL", Arrays.asList("ActiveTrial", "Paid", "PriceActive")),
        Arguments.of("customOffer1", "custom1-price-active",
            "ACTIVE", Arrays.asList("Active", "Paid", "PriceActive"))
      );  
    }

    @ParameterizedTest
    @MethodSource
    public void testCustomBundleComponent(String offerId, String priceId,
        String status, List<String> states) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(15);
      int tarificationPeriod = 2;
      JsonNode machineState = buildMachineState(states);
      String usage = states.get(0);
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", priceId,
        "component1", "component2", "component3")
        .tarificationPeriod(tarificationPeriod)
        .productStartDate(t0.minusDays(30))
        .activeEndDate(t1)
        .status(status)
        .machineState(machineState)
        .build();
      Product component3 = preBundle.getByOfferId("component3");
      TestBundle bundle = new BundleBuilder("component3", null)
        .productIds(Arrays.asList(component3))
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)  
        .tarificationPeriod(tarificationPeriod)
        .activeEndDate(t1)
        .addBundle(preBundle.bundle)
        .status(status)
        .build();

      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      // TODO: Добавить анализ на состав бандла
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .isIndependent(true)  // means leg becomes independently processed
        .tarificationPeriod(tarificationPeriod)
        .status("PENDING_DISCONNECT")
        .machineState(buildMachineState("PendingDisconnect", "PaymentFinal", "PriceFinal"))
        .build();

      TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t1);
      machine = createMachine(bundle);
      assertEquals(bundle.bundle.getProductId(), preBundle.bundle.getProductId());
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(Helper.stateSuite(usage, "PaymentFinal", "PriceFinal"))
                .and()
            .step()
                .sendEvent("deactivation_started")
                .expectStates(Helper.stateSuite("PendingDisconnect", "PaymentFinal", "PriceFinal"))
                .expectVariableWith(testBundleEqualTo(expectedBundle))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
    }

    public static Stream<Arguments> testCustomBundleWithComponentInPendingActivate() {
      return Stream.of(
        Arguments.of("custom1-price-trial", Arrays.asList("ActiveTrial", "Paid", "PriceActive")),
        Arguments.of("custom1-price-active", Arrays.asList("Active", "Paid", "PriceActive"))
      );
    }

    @ParameterizedTest
    @MethodSource
    public void testCustomBundleWithComponentInPendingActivate(String priceId, List<String> states) throws Exception {
      String offerId = "customBundleOffer1";
      List<String> componentOfferIds = Arrays.asList("component1", "component2", "component3");
      String independentOfferId = "component3";
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(15);
      OffsetDateTime waitPaymentTime = t0.plus(getWaitingPayInterval());
      String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
  
      TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .status(status)
        .machineState(buildMachineState(states))
        .productStartDate(t0.minusDays(30))
        .priceNextPayDate(t1)
        .activeEndDate(t1)
        .pricePeriod(0)
        .tarificationPeriod(2)
        .build();
      assertEquals(componentOfferIds.size(), bundle.components().size());
      // drive independent leg to PendingActivate state
      Product independent = bundle.getByOfferId(independentOfferId);
      independent.setStatus("PENDING_ACTIVATE");
      independent.setActiveEndDate(null);
      independent.getMachineContext().setIsIndependent(true);
      independent.setProductStartDate(t0);
      independent.getMachineContext().setMachineState(buildMachineState("PendingActivate"));
  
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .status("PENDING_DISCONNECT")
        .build();
      independent = expectedBundle.getByOfferId(independentOfferId);
      assert(independent != null);
      // Leg is independent yet and its parameters must stay unchanged
      independent.setActiveEndDate(null);
      independent.setStatus("PENDING_ACTIVATE");
      final String indProductId = independent.getProductId();
      // Other legs become independent
      expectedBundle.components().stream().filter(c -> ! c.getProductId().equals(indProductId))
        .forEach(c -> {
          c.getMachineContext().setIsIndependent(true);
          c.getMachineContext().setMachineState(buildMachineState("PendingDisconnect", "PaymentFinal", "PriceFinal"));
        });

      TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t1);
      // Change task type for independent leg (in PendingActivate): DISCONNECT -> ABORT
      expectedTasks.getCreatePlan().stream()
        .filter(t -> t.getProductId().equals(indProductId))
        .forEach(t -> { t.setType(TaskType.ABORT); t.setWakeAt(OffsetDateTime.now());} );
      
      machine = createMachine(bundle);
      
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(Helper.stateSuite(states))
                .and()
            .step()
                .sendEvent("deactivation_started")
                .expectStates(Helper.stateSuite("PendingDisconnect", "PaymentFinal", "PriceFinal"))
                .expectVariableWith(testBundleEqualTo(expectedBundle))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertEquals(null, bundle.getByOfferId(independentOfferId).getActiveEndDate());
      log.debug("states: {}", machine.getState().getIds());
    }

    @Test
    public void testPostponedToImmediate() throws Exception {
      String offerId = "component1";
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.plusDays(15);
      OffsetDateTime tstart = t0.minusDays(30);
      List<String> states = Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal");
      TestBundle bundle = new BundleBuilder(offerId, null)
        .status("PENDING_DISCONNECT")
        .machineState(buildMachineState(states))
        .productStartDate(tstart)
        .activeEndDate(t1)
        .build();
  
      String productId = bundle.drive.getProductId();
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .activeEndDate(t0)
        .build();
      TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t0);
  
      List<Characteristic> eventChars =  new ArrayList<>();
      Characteristic evch = new Characteristic();
      evch.setName(eventCharName);
      evch.setValue("Immediate");
      eventChars.add(evch);
  
      Message<String> message = MessageBuilder
          .withPayload("deactivation_started")
          .setHeader("characteristics", eventChars)
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
                .sendEvent(message)
                .expectStates(Helper.stateSuite(states))
                .expectVariableWith(testBundleEqualTo(expectedBundle))
                .expectVariableWith(taskPlanEqualTo(expectedTasks))
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
    }

    @Test
    public void testImmediateToPostponed() throws Exception {
      String offerId = "component1";
      OffsetDateTime t0 = OffsetDateTime.now();
      OffsetDateTime t1 = t0.minusDays(15);  // In past
      OffsetDateTime tstart = t0.minusDays(30);
      List<String> states = Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal");
      TestBundle bundle = new BundleBuilder(offerId, null)
        .status("PENDING_DISCONNECT")
        .machineState(buildMachineState(states))
        .productStartDate(tstart)
        .activeEndDate(t1)
        .build();
  
      String productId = bundle.drive.getProductId();
      // TestBundle expectedBundle = new BundleBuilder(bundle)
      //   .activeEndDate(t0)
      //   .build();
      // TaskPlan expectedTasks = createDefaultTaskPlan(expectedBundle, t0);
      // expectedTasks.getRemovePlan().add(TaskDef.builder().productId(productId).type(TaskType.DISCONNECT).build());
  
      List<Characteristic> eventChars =  new ArrayList<>();
      Characteristic evch = new Characteristic();
      evch.setName(eventCharName);
      evch.setValue("Postponed");
      eventChars.add(evch);
  
      Message<String> message = MessageBuilder
          .withPayload("deactivation_started")
          .setHeader("characteristics", eventChars)
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
                .sendEvent(message)
                .expectStateChanged(0)
                .expectEventNotAccepted(6)
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
    }
  }
}