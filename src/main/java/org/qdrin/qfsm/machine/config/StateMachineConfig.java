package org.qdrin.qfsm.machine.config;

import java.util.HashMap;
import java.util.Optional;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.*;
import org.qdrin.qfsm.machine.states.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.uml.UmlStateMachineModelFactory;

// @Slf4j
@Configuration
public class StateMachineConfig {

  @Bean
  public StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister(
          JpaStateMachineRepository jpaStateMachineRepository) {
      return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
  }

  @Bean
  public StateMachineService<String,String> stateMachineService(
        StateMachineFactory<String,String> stateMachineFactory,
        StateMachineRuntimePersister<String,String, String> stateMachineRuntimePersister) {
    return new DefaultStateMachineService<String, String>(stateMachineFactory, stateMachineRuntimePersister);
  }

  @Configuration
  @EnableStateMachineFactory
  public static class MachineConfig extends StateMachineConfigurerAdapter<String, String> {

		@Autowired
		private StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister;

    @Override
    public void configure(StateMachineModelConfigurer<String, String> model) throws Exception {
      model.withModel().factory(modelFactory());
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
      config
        .withPersistence()
        .runtimePersister(stateMachineRuntimePersister);
    }

    @Bean
    public StateMachineModelFactory<String, String> modelFactory() {
      UmlStateMachineModelFactory factory = new UmlStateMachineModelFactory("classpath:fsm/fsm.uml");
      // UmlStateMachineModelFactory factory = new UmlStateMachineModelFactory("classpath:fsm.simple/fsm.uml");
      return factory;
    }

  }

  @Configuration
  public static class SchemeBeans {
    // ------------------------------------------------------------------------------------------
    // actions
    @Bean
    EntryExit entryExit() {
      return new EntryExit();
    }

    @Bean
    PendingDisconnectEntry pendingDisconnectEntry() {
      return new PendingDisconnectEntry();
    }

    @Bean
    PaidEntry paidEntry() {
      return new PaidEntry();
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

    @Bean
    SignalAction sendCompletePrice() {
      return new SignalAction("complete_price");
    }

    @Bean
    SignalAction sendPaymentProcessed() {
      return new SignalAction("payment_processed");
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
    public Guard<String, String> prolongGuard() {
      return new ProlongGuard();
    }

    @Bean
    public Guard<String, String> notFullPrice() {
      return new PriceGuard(Optional.of(false), Optional.empty());
    }

    @Bean
    public Guard<String, String> newPrice() {
      return new PriceGuard(Optional.of(true), Optional.of(false));
    }

    @Bean
    public Guard<String, String> samePrice() {
      return new PriceGuard(Optional.of(true), Optional.of(true));
    }
  }
}
