package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.*;
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

  private static Stream<Arguments> provideActivationStartedData() {
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", ProductClass.SIMPLE, null, 
          null,
          "PENDING_ACTIVATE"),
      Arguments.of("simpleOffer1", "simple1-price-active", ProductClass.SIMPLE, null,
          null,
          "PENDING_ACTIVATE"),
      Arguments.of("bundleOffer1", "bundle1-price-trial", ProductClass.BUNDLE, null,
          new String[] {"component1", "component2", "component3"},
          "PENDING_ACTIVATE"),
      Arguments.of("bundleOffer1", "bundle1-price-active", ProductClass.BUNDLE, null, 
          new String[] {"component1", "component2", "component3"},
          "PENDING_ACTIVATE"),
      Arguments.of("customBundleOffer1", "custom1-price-trial", ProductClass.CUSTOM_BUNDLE, null,
          new String[] {"component1", "component2", "component3"},
          "PENDING_ACTIVATE"),
      Arguments.of("customBundleOffer1", "cutsom1-price-active", ProductClass.CUSTOM_BUNDLE, null, 
          new String[] {"component1", "component2", "component3"},
          "PENDING_ACTIVATE")
    );
  }
  
  @ParameterizedTest
  @MethodSource("provideActivationStartedData")
  public void testSuccess(String offerId, String priceId, ProductClass driveClass, String[] machineState,
            String[] componentOfferIds, String expectedStatus) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    ProductClass componentClass;
    switch(driveClass) {
      case BUNDLE:
        componentClass = ProductClass.BUNDLE_COMPONENT;
        break;
      case CUSTOM_BUNDLE:
        componentClass = ProductClass.CUSTOM_BUNDLE_COMPONENT;
        break;
      default:
        componentClass = ProductClass.VOID;
    }
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .productStartDate(t0.minusSeconds(30))
      .machineState(Helper.buildMachineState(machineState))
      .build();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(0)
      .driveClass(driveClass)
      .componentClass(componentClass)
      .status(expectedStatus)
      .productStartDate(t0)
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
    assertEquals(expectedStatus, bundle.drive.getStatus());
    Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
    Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
  }

  @Nested
  class CustomBundleComponent {
    @Test
    public void testSuccessToPendingActivate() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", "custom1-price-trial",
        "component1", "component2")
        .tarificationPeriod(0)
        .build();

      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .addBundle(preBundle.bundle)
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("PENDING_ACTIVATE")
        .build();
      machine = createMachine(bundle);
      assertEquals(bundle.bundle.getProductId(), preBundle.bundle.getProductId());
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
      Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
    }

    @Test
    public void testSuccessToActive() throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", "custom1-price-trial",
        "component1", "component2")
        .machineState(Helper.buildMachineState("Active", "Paid", "PriceActive"))
        .tarificationPeriod(0)
        .build();

      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .addBundle(preBundle.bundle)
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("PENDING_ACTIVATE")
        .build();
      machine = createMachine(bundle);
      assertEquals(bundle.bundle.getProductId(), preBundle.bundle.getProductId());
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
      Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
    }
  }
}
