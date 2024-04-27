package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.access.StateMachineAccess;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.Product;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

@SpringBootTest
public class StateMachineTest {

  @Autowired
  private StateMachineFactory<String, String> stateMachineFactory;

  private StateMachine<String, String> machine;

  @BeforeEach
  public void setup() throws Exception {
    machine = stateMachineFactory.getStateMachine();
  }

  @Test
  public void testInitial() throws Exception {
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Aborted")
              .and()
          .build();
        plan.test();
  }

  @Test
  public void testSimpleStateSet() throws Exception {
    var accessor = machine.getStateMachineAccessor();
    StateMachineContext<String, String> context = new DefaultStateMachineContext<String,String>("Aborted", null, null, null);
    Consumer<StateMachineAccess<String, String>> access;
    access = arg -> {
      arg.resetStateMachineReactively(context).block();
    }; 
    accessor.doWithAllRegions(access);

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Aborted")
              .and()
          .build();
        plan.test();
  }

  @Test
  public void testActivationStartedSuccess() throws Exception {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active").build();
    machine.getExtendedState().getVariables().put("product", product);
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
  }
}
