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
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.PriceType;

import static org.qdrin.qfsm.Helper.Assertions.*;
import static org.qdrin.qfsm.Helper.buildMachineState;
import static org.qdrin.qfsm.TaskPlanEquals.taskPlanEqualTo;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ChangePriceCompletedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  public static Stream<Arguments> testData() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", "simple1-price-active", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-trial", "bundle1-price-active", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-trial", "custom1-price-active", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("simpleOffer1", "simple1-price-active", "simple1-price-trial", new ArrayList<>()),
      Arguments.of("bundleOffer1", "bundle1-price-active", "bundle1-price-trial", Arrays.asList("component1", "component2", "component3")),
      Arguments.of("customBundleOffer1", "custom1-price-active", "custom1-price-trial", Arrays.asList("component1", "component2", "component3"))
    );
  }

  @ParameterizedTest
  @MethodSource("testData")
  // Prolong
  public void testWithNoPayment(String offerId, String priceId, String userPriceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    OffsetDateTime trialEndDate = priceId.contains("trial") ? t0 : null;
    String usage = priceId.contains("trial") ? "ActiveTrial" : "Active";
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
    List<String> states = Arrays.asList(usage, "WaitingPayment", "PriceChanged");
    List<String> expectedStates = Arrays.asList(usage, "WaitingPayment", "PriceActive");
    
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(states))
      .productStartDate(t0)
      .priceNextPayDate(t0)
      .activeEndDate(t0)
      .trialEndDate(trialEndDate)
      .pricePeriod(2)
      .tarificationPeriod(2)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    ProductPrice userPrice = Helper.testOffers.getOffers().get(offerId).getPrice(userPriceId);
    userPrice.setNextPayDate(t1);  // TODO: we cannot trust foreign dates
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(status)
      .machineState(buildMachineState(expectedStates))
      .pricePeriod(0)
      .activeEndDate(t1)
      .build();
    expectedBundle.drive.setProductPrice(Arrays.asList(userPrice));

    Message<String> message = MessageBuilder.withPayload("change_price_completed")
      .setHeader("userPrice", Arrays.asList(userPrice))
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
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent(message)
              .expectStates(Helper.stateSuite(expectedStates))
              .expectVariableWith(taskPlanEqualTo(expectedTasks))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  @ParameterizedTest
  @MethodSource("testData")
  // Prolong
  public void testWithPayment(String offerId, String priceId, String userPriceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    OffsetDateTime trialEndDate = priceId.contains("trial") ? t0 : null;
    String usage = priceId.contains("trial") ? "ActiveTrial" : "Active";
    String status = priceId.contains("trial") ? "ACTIVE_TRIAL" : "ACTIVE";
    List<String> states = Arrays.asList(usage, "Paid", "PriceChanged");
    List<String> expectedStates = Arrays.asList("Prolongation", "Paid", "PriceActive");
    
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .status(status)
      .machineState(buildMachineState(states))
      .productStartDate(t0)
      .priceNextPayDate(t0)
      .activeEndDate(t0)
      .trialEndDate(trialEndDate)
      .pricePeriod(2)
      .tarificationPeriod(2)
      .build();
    assertEquals(componentOfferIds.size(), bundle.components().size());
    ProductPrice userPrice = Helper.testOffers.getOffers().get(offerId).getPrice(userPriceId);
    userPrice.setNextPayDate(t1);  // TODO: we cannot trust foreign dates
    String productId = bundle.drive.getProductId();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status(status)
      .machineState(buildMachineState(expectedStates))
      .pricePeriod(0)
      .activeEndDate(t1)
      .build();
    expectedBundle.drive.setProductPrice(Arrays.asList(userPrice));

    Message<String> message = MessageBuilder.withPayload("change_price_completed")
      .setHeader("userPrice", Arrays.asList(userPrice))
      .build();
    machine = createMachine(bundle);
    
    TaskPlan expectedTasks = new TaskPlan(productId);
    expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.PRICE_ENDED)
      .wakeAt(t1.minus(getPriceEndedBefore()))
      .build()
    );
    expectedTasks.addToCreatePlan(TaskDef.builder()
      .type(TaskType.PROLONG_EXTERNAL)
      .build()
    );

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite(states))
              .and()
          .step()
              .sendEvent(message)
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
