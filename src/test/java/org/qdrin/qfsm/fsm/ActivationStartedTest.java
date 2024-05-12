package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;
import static org.qdrin.qfsm.Helper.Assertions.assertProductEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
  
  public static Stream<Arguments> testSuccess() {
    List<String> components = Arrays.asList("component1", "component2", "component3");
    List<String> empty = Arrays.asList();
    return Stream.of(
      Arguments.of("simpleOffer1", "simple1-price-trial", empty),
      Arguments.of("simpleOffer1", "simple1-price-active", empty),
      Arguments.of("bundleOffer1", "bundle1-price-trial", components),
      Arguments.of("bundleOffer1", "bundle1-price-active", components),
      Arguments.of("customBundleOffer1", "custom1-price-trial", components),
      Arguments.of("customBundleOffer1", "custom1-price-active", components)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testSuccess(String offerId, String priceId, List<String> componentOfferIds) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    String expectedStatus = "PENDING_ACTIVATE";
    ProductClass driveClass = ProductClass.SIMPLE;
    if(! offerId.contains("simple")) {
      driveClass = offerId.contains("custom") ? ProductClass.CUSTOM_BUNDLE : ProductClass.BUNDLE;
    }
    ProductClass componentClass = Helper.getComponentClass(driveClass);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .productStartDate(t0.minusSeconds(30))
      .build();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .tarificationPeriod(0)
      .status(expectedStatus)
      .productStartDate(t0)
      .build();
    ProductClass actualClass = ProductClass.values()[bundle.drive.getProductClass()];
    assertEquals(driveClass, actualClass);
    log.debug("expectedBundle: {}", expectedBundle);
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
    releaseMachine(machine.getId());
    assertEquals(expectedStatus, bundle.drive.getStatus());
    Helper.Assertions.assertProductEquals(expectedBundle.drive, bundle.drive);
    Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
  }

  @Nested
  class CustomBundleComponent {
    @ParameterizedTest
    @ValueSource(strings = {"custom1-price-trial", "custom1-price-active"})
    public void testSuccessToPendingActivate(String priceId) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      String offerId = "customBundleOffer1";
      String[] componentOfferIds = new String[] {"component1", "component2"};

      TestBundle preBundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .tarificationPeriod(0)
        .status("PENDING_ACTIVATE")
        .build();

      log.debug("preBundle.bundle: {}", preBundle.bundle);
      assert(preBundle.bundle.getMachineContext().getIsIndependent());
      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .addBundle(preBundle.bundle)
        .build();
      // Check BundleBuilder relations
      assertEquals(componentOfferIds.length, bundle.bundle.getProductRelationship().size());

      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("PENDING_ACTIVATE")
        .isIndependent(true)
        .build();
      expectedBundle.bundle.getProductRelationship().add(new ProductRelationship(bundle.drive));
      assert(expectedBundle.bundle.getMachineContext().getIsIndependent()) : "expectedBundle is not independent";
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
      releaseMachine(machine.getId());
      assertEquals(product.getStatus(), "PENDING_ACTIVATE");
      log.debug("bundle relations: {}", bundle.bundle.getProductRelationship());
      assertProductEquals(expectedBundle.drive, product);
      assertProductEquals(expectedBundle.bundle, bundle.bundle);
    }
  }
}
