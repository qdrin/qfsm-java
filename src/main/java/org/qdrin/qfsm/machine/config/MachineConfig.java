package org.qdrin.qfsm.machine.config;

import java.util.HashMap;
import java.util.Optional;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.*;
import org.qdrin.qfsm.machine.states.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.RepositoryStateMachinePersist;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.uml.UmlStateMachineModelFactory;
import org.springframework.stereotype.Repository;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MachineConfig {

  @Configuration
  @EntityScan("org.springframework.statemachine.data.jpa")
  @EnableJpaRepositories("org.springframework.statemachine.data.jpa")
  public static class JpaPersisterConfig {

    @Bean
    public StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister(
        JpaStateMachineRepository jpaStateMachineRepository) {
      return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }
  }

  @Bean
	public StateMachineService<String, String> stateMachineService(
					final StateMachineModelFactory<String, String> stateMachineFactory,
					final StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister) {
			return new DefaultStateMachineService<>((StateMachineFactory) stateMachineFactory, stateMachineRuntimePersister);
	}

  public static class InMemoryStateMachinePersist implements StateMachinePersist<String, String, String> {
    private final HashMap<String, StateMachineContext<String, String>> contexts = new HashMap<>();

    @Override
    public void write(StateMachineContext<String, String> context, String id) throws Exception {
      contexts.put(id, context);
    }
    
    @Override
    public StateMachineContext<String, String> read(String id) throws Exception {
      return contexts.get(id);
    }
  }


  @Configuration
  @EnableStateMachine
  public static class StateMachineConfig extends StateMachineConfigurerAdapter<String, String>{
    
    @Override
    public void configure(StateMachineModelConfigurer<String, String> model) throws Exception {
      model.withModel().factory(modelFactory());
    }

    @Bean
    public StateMachineModelFactory<String, String> modelFactory() {
      return new UmlStateMachineModelFactory("classpath:fsm/fsm.uml");
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
