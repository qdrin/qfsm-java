package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.Assert.*;
import static org.qdrin.qfsm.Helper.Assertions.assertProductEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.TestSetup;
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
  
  @ParameterizedTest
  @MethodSource("org.qdrin.qfsm.Helper#provideTestSetups")
  public void testSuccess(TestSetup argument) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    String expectedStatus = "PENDING_ACTIVATE";
    ProductClass driveClass = argument.getProductClass();
    String priceId = argument.getPriceId();
    String offerId = argument.getOfferId();
    String[] componentOfferIds = argument.getComponentOfferIds().toArray(new String[0]);

    ProductClass componentClass = Helper.getComponentClass(driveClass);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .productStartDate(t0.minusSeconds(30))
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
      String offerId = "customBundleOffer1";
      String priceId = "custom1-price-trial";
      String[] componentOfferIds = new String[] {"component1", "component2"};

      TestBundle preBundle = new BundleBuilder(offerId, priceId, componentOfferIds)
        .tarificationPeriod(0)
        .build();

      log.debug("preBundle.bundle: {}", preBundle.bundle);
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
        .build();
      List<ProductRelationship> expectedRelations = expectedBundle.bundle.getProductRelationship();
      ProductRelationship componentRelation = new ProductRelationship(); 
      componentRelation.setProductId(bundle.drive.getProductId());
      componentRelation.setProductOfferingId(offerId);
      componentRelation.setRelationshipType("CUSTOM_BUNDLES");
      expectedRelations.add(componentRelation);

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
      log.debug("bundle relations: {}", bundle.bundle.getProductRelationship());
      assertEquals(expectedRelations.size(), bundle.bundle.getProductRelationship().size());
      assertProductEquals(expectedBundle.drive, product);
      assertProductEquals(expectedBundle.bundle, bundle.bundle);
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
