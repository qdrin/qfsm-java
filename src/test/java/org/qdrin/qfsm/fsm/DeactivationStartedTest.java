package org.qdrin.qfsm.fsm;

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
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import static org.qdrin.qfsm.TestBundleEquals.testBundleEqualTo;
import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DeactivationStartedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  private static TaskPlan createDefaultTaskPlan(TestBundle bundle, OffsetDateTime activeEndDate) {
    TaskPlan tasks = new TaskPlan(bundle.drive.getProductId());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
    tasks.addToRemovePlan(TaskDef.builder().type(TaskType.RESUME_EXTERNAL).build());

    for(Product product: bundle.products) {
      tasks.addToCreatePlan(TaskDef.builder()
        .productId(product.getProductId())
        .type(TaskType.DISCONNECT)
        .wakeAt(activeEndDate)
        .build());
    }

    return tasks;
  }

  public static Stream<Arguments> testPostponed() {
    List<String> components = Arrays.asList("component1", "component2", "component3");
    List<String> empty = new ArrayList<>();

    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Active", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", 
        Arrays.asList("Active", "Paid", "PriceActive"), components),

      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Prolongation", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", 
        Arrays.asList("Prolongation", "Paid", "PriceActive"), components),

      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Resuming", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", 
        Arrays.asList("Resuming", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), components),
      Arguments.of("customBundleOffer1", "custom1-price-active", 
        Arrays.asList("Resuming", "Paid", "PriceActive"), components)
      );
  }
  @ParameterizedTest
  @MethodSource
  public void testPostponed(String offerId, String priceId, List<String> states,
        List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(15);
    OffsetDateTime tstart = t0.minusDays(30);
    String status = states.get(0).equals("ActiveTrial") ? "ACTIVE_TRIAL" : "ACTIVE";
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(states))
      .productStartDate(tstart)
      .activeEndDate(t1)
      .pricePeriod(1)
      .tarificationPeriod(2)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("PENDING_DISCONNECT")
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

  @Nested
  class CustomBundle {

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
  }
}