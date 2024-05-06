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
import org.qdrin.qfsm.ProductClass;
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
      Product product = bundle.drive;
      List<Product> components = bundle.components;
      assertEquals(ProductClass.BUNDLE.ordinal(), product.getProductClass());
      assertEquals(ProductClass.BUNDLE_COMPONENT.ordinal(), components.get(0).getProductClass());

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
                .sendEvent("activation_started")
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.components, components);
    }

    @Test
    public void testActiveSuccess() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("bundleOffer1", "bundle1-price-active",
        "component1", "component2", "component3")
        .tarificationPeriod(0)
        .build();
      Product product = bundle.drive;
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
                .sendEvent("activation_started")
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.components, components);
    }

    @Test
    public void testFailAddComponent() throws Exception {
      assert(false);
    }
  }

  @Nested
  class CustomBundle {
    @Test
    public void testTrialSuccess() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("customBundleOffer1", "custom1-price-trial",
        "component1", "component2", "component3")
        .tarificationPeriod(0)
        .build();
      Product product = bundle.drive;
      List<Product> components = bundle.components;
      assertEquals(ProductClass.CUSTOM_BUNDLE.ordinal(), product.getProductClass());
      assertEquals(ProductClass.CUSTOM_BUNDLE_COMPONENT.ordinal(), components.get(0).getProductClass());
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
                .sendEvent("activation_started")
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.components, components);
    }

    @Test
    public void testActiveSuccess() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("customBundleOffer1", "custom1-price-active",
        "component1", "component2", "component3")
        .tarificationPeriod(0)
        .build();
      Product product = bundle.drive;
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
                .sendEvent("activation_started")
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.components, components);
    }
  }

  @Nested
  class CustomBundleComponent {
    @Test
    public void testSuccessToPendingActivate() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle bundle = new BundleBuilder("customBundleOffer1", "custom1-price-trial",
        "component1", "component2")
        .tarificationPeriod(0)
        .build();

      Product product = new ProductBuilder("component1", "", null)
        .productClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .build();
      bundle.drive = product;

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
                .sendEvent("activation_started")
                .expectState("PendingActivate")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.bundle, bundle.bundle);
      Helper.Assertions.assertProductEquals(expectedBundle.components, bundle.components);
    }
  }
}
