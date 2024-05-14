package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.support.AbstractStateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.Collection;

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

  public static Stream<Arguments> testPostponed() {
    List<String> components = Arrays.asList("component1", "component2", "component3");
    List<String> empty = new ArrayList<>();
    List<List<String>> possibleStates = Arrays.asList(
      Arrays.asList("ActiveTrial", "Paid", "PriceActive"),
      Arrays.asList("Prolongation", "Paid", "PriceActive"),
      Arrays.asList("Resuming", "Paid", "PriceActive")
    );
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Active", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("ActiveTrial", "Paid", "PriceActive"), 
      Arguments.of("customBundleOffer1", "custom1-price-active", components)),

      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("Prolongation", "Paid", "PriceActive"), 
      Arguments.of("customBundleOffer1", "custom1-price-active", components)),

      Arguments.of("simpleOffer1", "simple1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), empty),
      Arguments.of("simpleOffer1", "simple1-price-active",
        Arrays.asList("Resuming", "Paid", "PriceActive"), empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), components),
      Arguments.of("bundleOffer1", "bundle1-price-active", components),
      Arguments.of("customBundleOffer1", "custom1-price-trial",
        Arrays.asList("Resuming", "Paid", "PriceActive"), 
      Arguments.of("customBundleOffer1", "custom1-price-active", components))
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
      .machineState(Helper.buildMachineState(states))
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
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.PRICE_ENDED).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.SUSPEND_ENDED).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.WAITING_PAY_ENDED).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.CHANGE_PRICE).build());
    expectedTasks.addToRemovePlan(TaskDef.builder().type(TaskType.RESUME_EXTERNAL).build());
    for(Product product: bundle.products) {
      expectedTasks.addToCreatePlan(TaskDef.builder()
        .productId(product.getProductId())
        .type(TaskType.DISCONNECT)
        .wakeAt(t1)
        .build());
    }

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