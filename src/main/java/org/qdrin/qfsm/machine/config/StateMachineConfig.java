package org.qdrin.qfsm.machine.config;

import org.qdrin.qfsm.machine.StateLogAction;
import org.qdrin.qfsm.machine.guards.ActivatedGuard;
import org.qdrin.qfsm.machine.states.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
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

  @Bean
  public StateLogAction stateLogAction() {
    return new StateLogAction();
  }

  @Bean
  PendingDisconnectEntry pendingDisconnectEntry() {
    return new PendingDisconnectEntry();
  }

  @Bean
  SignalAction signalAction() {
    String signal = "suspend";
    return new SignalAction(signal);
  }

  @Bean
  public ActivatedGuard activeGuard() {
    return new ActivatedGuard("ACTIVE");
  }

  @Bean
  public ActivatedGuard activeTrialGuard() {
    return new ActivatedGuard("ACTIVE_TRIAL");
  }
}
