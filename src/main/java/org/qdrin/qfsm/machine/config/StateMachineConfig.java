package org.qdrin.qfsm.machine.config;

import org.qdrin.qfsm.machine.StateLogAction;
import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.ActivatedGuard;
import org.qdrin.qfsm.machine.states.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.uml.UmlStateMachineModelFactory;

@Configuration
@EnableStateMachine
public class StateMachineConfig extends StateMachineConfigurerAdapter<String, String>{
  
  @Override
  public void configure(StateMachineModelConfigurer<String, String> model) throws Exception {
    model.withModel().factory(modelFactory());
  }

  @Bean
  public StateMachineModelFactory<String, String> modelFactory() {
    return new UmlStateMachineModelFactory("classpath:fsm/fsm.uml");
    // return new UmlStateMachineModelFactory("classpath:uml_sample/simple-forkjoin.uml");
  }

  // ------------------------------------------------------------------------------------------
  // actions
  @Bean
  public StateLogAction stateLogAction() {
    return new StateLogAction();
  }

  @Bean
  PendingDisconnectEntry pendingDisconnectEntry() {
    return new PendingDisconnectEntry();
  }

  @Bean
  PriceActiveEntry priceActiveEntry() {
    return new PriceActiveEntry();
  }

  @Bean
  SignalAction sendSuspend() {
    return new SignalAction("suspend");
  }

  @Bean
  SignalAction sendResume() {
    return new SignalAction("resume");
  }

  @Bean
  SignalAction sendWaitPayment() {
    return new SignalAction("wait_payment");
  }

  @Bean
  SignalAction sendProlong() {
    return new SignalAction("prolong");
  }


  // -----------------------------------------------------------------
  // guards
  @Bean
  public ActivatedGuard isActive() {
    return new ActivatedGuard("ACTIVE");
  }

  @Bean
  public ActivatedGuard isTrial() {
    return new ActivatedGuard("ACTIVE_TRIAL");
  }

  @Bean
  public Guard<String, String> notFirstPeriod() {
    return new Guard<String, String>() {
      @Override
      public boolean evaluate(StateContext<String, String> context) {
        int period = (int) context.getExtendedState().getVariables().get("tarificationPeriod");
        return period > 1;
      }
    };
  }
}
