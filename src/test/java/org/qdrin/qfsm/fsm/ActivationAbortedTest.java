package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.buildMachineState;
import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.Helper.Assertions.*;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.TestSetup;
import org.qdrin.qfsm.model.*;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ActivationAbortedTest extends SpringStarter {

  StateMachine<String, String> machine = null;
  
  @BeforeEach
  public void setup() throws Exception {
    machine = null;
    clearDb();
  }

  @ParameterizedTest
  @MethodSource("org.qdrin.qfsm.Helper#provideTestSetups")
  public void testSuccess(TestSetup arg) throws Exception {
    OffsetDateTime t0 = OffsetDateTime.now();
    OffsetDateTime t1 = t0.plusDays(30);
    String offerId = arg.getOfferId();
    ProductClass driveClass = arg.getProductClass();
    String priceId = arg.getPriceId();
    List<String> componentIds = arg.getComponentOfferIds();
    
    ProductClass componentClass = Helper.getComponentClass(driveClass);
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentIds)
      .productStartDate(t0)
      .tarificationPeriod(0)
      .status("PENDING_ACTIVATE")
      .machineState(Helper.buildMachineState("PendingActivate"))
      .pricePeriod(0)
      .priceNextPayDate(t1)
      .build();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("ABORTED")
      .machineState(Helper.buildMachineState("Aborted"))
      .driveClass(driveClass)
      .componentClass(componentClass)
      .tarificationPeriod(0)
      .pricePeriod(0)
      .build();
    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .sendEvent("activation_aborted")
              .expectStates(Helper.stateSuite("Aborted"))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    log.debug("states: {}", machine.getState().getIds());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }

  @Nested
  class CustomBundleComponent {

    @ParameterizedTest
    @ValueSource(strings = {"custom1-price-trial", "custom1-price-active"})

    public void testSuccess(String priceId) throws Exception {
      OffsetDateTime t0 = OffsetDateTime.now();
      TestBundle preBundle = new BundleBuilder("customBundleOffer1", priceId,
        "component1", "component2")
        .tarificationPeriod(0)
        .productStartDate(t0)
        .build();

      TestBundle bundle = new BundleBuilder("component3", null)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .addBundle(preBundle.bundle)
        .machineState(Helper.buildMachineState("PendingActivate"))
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("ABORTED")
        .machineState(Helper.buildMachineState("Aborted"))
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
                .sendEvent("activation_aborted")
                .expectState("Aborted")
                .expectStateChanged(1)
                .and()
            .build();
      plan.test();
      releaseMachine(machine.getId());
      assertEquals(product.getStatus(), "ABORTED");
      Helper.Assertions.assertProductEquals(expectedBundle.drive, product);
      Helper.Assertions.assertProductEquals(expectedBundle.bundle, bundle.bundle);
      Helper.Assertions.assertProductEquals(expectedBundle.components(), bundle.components());
    }
  }
}
