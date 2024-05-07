package org.qdrin.qfsm.fsm;

import org.springframework.statemachine.StateMachine;

import static org.qdrin.qfsm.Helper.Assertions.*;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
    
    ProductClass componentClass = Helper.getComponentClass(driveClass);
    TestBundle bundle = new BundleBuilder(offerId, priceId)
      .productStartDate(t0)
      .tarificationPeriod(0)
      .machineState(Helper.buildMachineState("PendingActivate"))
      .build();
    ProductPrice price = bundle.drive.getProductPrice().get(0);
    price.setNextPayDate(t1);
    price.setPeriod(0);
    TestBundle expectedBundle = new BundleBuilder(bundle)
      .status("ABORTED")
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
              .expectStates(Helper.stateSuit("Aborted"))
              .and()
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    assertProductEquals(expectedBundle.drive, bundle.drive);
    assertProductEquals(expectedBundle.components(), bundle.components());
  }
}