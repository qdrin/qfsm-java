package org.qdrin.qfsm.machine.config;

import org.qdrin.qfsm.machine.StateLogAction;
import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.ActivatedGuard;
import org.qdrin.qfsm.machine.guards.SamePriceGuard;
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
  PriceChangingEntry priceChangingEntry() {
    return new PriceChangingEntry();
  }

  @Bean
  PriceChangedEntry priceChangedEntry() {
    return new PriceChangedEntry();
  }

  @Bean
  PriceNotChangedEntry priceNotChangedEntry() {
    return new PriceNotChangedEntry();
  }

  @Bean
  SuspendingEntry suspendingEntry() {
    return new SuspendingEntry();
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

  @Bean
  SignalAction sendPriceEnded() {
    return new SignalAction("price_ended");
  }

  @Bean
  SignalAction sendChangePrice() {
    return new SignalAction("change_price");
  }

  @Bean
  SignalAction sendChangePriceCompleted() {
    return new SignalAction("change_price_completed");
  }

  @Bean
  SignalAction sendResumePrice() {
    return new SignalAction("resume_price");
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

  @Bean
  public Guard<String, String> samePrice() {
    return new SamePriceGuard(true);
  }

  // @Bean
  // public Guard<String, String> notSamePrice() {
  //   return new SamePriceGuard(false);
  // }
}
