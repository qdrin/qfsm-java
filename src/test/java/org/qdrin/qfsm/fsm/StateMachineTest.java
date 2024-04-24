package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.config.StateMachineFactory;
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
    for(int i = 0; i < 10; i++) {
      if(machine.getState() != null) {
        break;
      }
      Thread.sleep(200);
    }
  }

  @Test
  public void testInitial() throws Exception {
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .build();
        plan.test();
  }

  @Test
  public void testActivationStartedSuccess() throws Exception {
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
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
  }
  
}
