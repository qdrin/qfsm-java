package org.qdrin.qfsm.fsm;

import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationStartedTest extends SpringStarter {

  StateMachine<String, String> machine = null;

  @BeforeEach
  public void setup() throws Exception {
    machine = null;
  }

  @AfterEach
  public void tearDown() throws Exception {
    machine.stopReactively().block();
    clearDb();
  }

  @Nested
  class Simple {
    @Test
    public void testTrialSuccess() throws Exception {
      Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
      OffsetDateTime t0 = OffsetDateTime.now();
      Product expectedProduct = new ProductBuilder(product)
        .tarificationPeriod(0)
        .status("PENDING_ACTIVATE")
        .productStartDate(t0)
        .build();
      machine = createMachine(product);
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Entry")
                .and()
            .step()
                .sendEvent(MessageBuilder.withPayload("activation_started")
                    .setHeader("product", product)
                    .setHeader("datetime", OffsetDateTime.now())
                    .build())
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedProduct, product);
    }

    @Test
    public void testActiveSuccess() throws Exception {
      Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active").build();
      OffsetDateTime t0 = OffsetDateTime.now();
      Product expectedProduct = new ProductBuilder(product)
        .tarificationPeriod(0)
        .status("PENDING_ACTIVATE")
        .productStartDate(t0)
        .build();
      machine = createMachine(product);
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Entry")
                .and()
            .step()
                .sendEvent(MessageBuilder.withPayload("activation_started")
                    .setHeader("product", product)
                    .setHeader("datetime", OffsetDateTime.now())
                    .build())
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedProduct, product);
    }
  }

  @Nested
  class Bundle {
    @Test
    public void testTrialSuccess() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("bundleOffer1", "bundle1-price-trial",
        "component1", "component2", "component3")
        .tarificationPeriod(0)
        .build();
      Product product = bundle.bundle;
      List<Product> components = bundle.components;
      TestBundle expectedBundle = new BundleBuilder(bundle.products())
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("PENDING_ACTIVATE")
        .build();
      machine = createMachine(bundle);
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Entry")
                .and()
            .step()
                .sendEvent(MessageBuilder.withPayload("activation_started")
                    .setHeader("product", product)
                    .setHeader("datetime", OffsetDateTime.now())
                    .build())
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.bundle, product);
      Helper.Assertions.assertProductEquals(expectedBundle.components, components);
    }

    @Test
    public void testActiveSuccess() throws Exception {
      Product product = new ProductBuilder("bundleOffer1", "", "bundle1-price-active").build();
      OffsetDateTime t0 = OffsetDateTime.now();
      Product expectedProduct = new ProductBuilder(product)
        .tarificationPeriod(0)
        .status("PENDING_ACTIVATE")
        .productStartDate(t0)
        .build();
      machine = createMachine(product);
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Entry")
                .and()
            .step()
                .sendEvent(MessageBuilder.withPayload("activation_started")
                    .setHeader("product", product)
                    .setHeader("datetime", OffsetDateTime.now())
                    .build())
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedProduct, product);
    }    
  }

  @Test
  public void testActivationCompletedSuccess() throws Exception {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    ProductPrice price = product.getProductPrice().get(0);
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    machine = createMachine(product);

    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
    List<ActionSuite> deleteActions = (List<ActionSuite>) variables.get("deleteActions");

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
              .expectStates(Helper.stateSuit("ActiveTrial", "Paid", "PriceActive"))
              .and()
          .defaultAwaitTime(1)
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    log.debug("actions: {}, deleteActions: {}", actions, deleteActions);
    assertEquals(product.getStatus(), "ACTIVE_TRIAL");
    assert(actions.contains(ActionSuite.PRICE_ENDED));
    assert(! actions.contains(ActionSuite.WAITING_PAY_ENDED));
    assert(! deleteActions.contains(ActionSuite.WAITING_PAY_ENDED));
  }
}
