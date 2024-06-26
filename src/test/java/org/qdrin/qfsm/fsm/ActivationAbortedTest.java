package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.Helper;
import static org.qdrin.qfsm.Helper.buildMachineState;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.SpringStarter;
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
    OffsetDateTime t1 = t0.plusDays(30);
    
    TestBundle bundle = new BundleBuilder(offerId, priceId, componentOfferIds)
      .productStartDate(t0)
      .tarificationPeriod(0)
      .status("PENDING_ACTIVATE")
      .machineState(buildMachineState("PendingActivate"))
      .pricePeriod(0)
      .priceNextPayDate(t1)
      .build();
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("ABORTED")
      .machineState(buildMachineState("Aborted"))
      .componentMachineState(buildMachineState("Aborted"))
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
        .machineState(buildMachineState("PendingActivate"))
        .build();
      log.debug("bundle: {}", bundle);
      Product product = bundle.drive;
      TestBundle expectedBundle = new BundleBuilder(bundle)
        .driveClass(ProductClass.CUSTOM_BUNDLE_COMPONENT)
        .tarificationPeriod(0)
        .productStartDate(t0)
        .status("ABORTED")
        .machineState(buildMachineState("Aborted"))
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
