package org.qdrin.qfsm.machine.config;

import java.util.EnumSet;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.qdrin.qfsm.machine.Events;
import org.qdrin.qfsm.machine.States;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableStateMachine
@Slf4j
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<States, Events>{

  @Override
  public void configure(StateMachineConfigurationConfigurer<States, Events> config) throws Exception {
    config.withConfiguration()
      .autoStartup(true)
      .listener(listener());
  }


  @Override
  public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
    states.withStates()
      .initial(States.PendingActivate)
      .states(EnumSet.allOf(States.class));
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
    transitions.withExternal()
      .source(States.PendingActivate).target(States.Active).event(Events.activate)
    .and().withExternal()
      .source(States.Active).target(States.Disconnected).event(Events.disconnect)
    .and().withExternal()
      .source(States.Active).target(States.Suspended).event(Events.suspend)
    .and().withExternal()
      .source(States.Suspended).target(States.Active).event(Events.resume);
  }

  @Bean
  public StateMachineListener<States, Events> listener() {
    return new StateMachineListenerAdapter<States, Events>() {
      @Override
      public void stateChanged(State<States, Events> from, State<States, Events> to) {
        log.info("State: {}", to.getId());
      }
    };
  }
}
