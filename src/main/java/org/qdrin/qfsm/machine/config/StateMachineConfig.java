package org.qdrin.qfsm.machine.config;

import org.qdrin.qfsm.machine.StateLogAction;
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
  }

  @Bean
  public StateLogAction stateLogAction() {
    return new StateLogAction();
  }
}
