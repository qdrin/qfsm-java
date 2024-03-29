package org.qdrin.qfsm.machine.config;

import java.util.Optional;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.*;
import org.qdrin.qfsm.machine.states.*;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.service.QStateMachineService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.uml.UmlStateMachineModelFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StateMachineConfig {

	@Bean
	ProductStateMachinePersist stateMachinePersist() {
		return new ProductStateMachinePersist();
	}

  @Bean
  StateMachinePersister<String, String, String> stateMachinePersister() {
    return new DefaultStateMachinePersister<>(stateMachinePersist());
  }

  @Bean 
  StateMachineService<String, String> stateMachineService(
      StateMachineFactory<String, String> factory
  ) {
    return new QStateMachineService<>(factory, stateMachinePersister());
  }

  @Configuration
  // @EnableStateMachine
  @EnableStateMachineFactory
  public static class MachineConfig extends StateMachineConfigurerAdapter<String, String> {

    @Override
    public void configure(StateMachineModelConfigurer<String, String> model) throws Exception {
      model.withModel().factory(modelFactory());
    }

    @Bean
    public StateMachineModelFactory<String, String> modelFactory() {
      UmlStateMachineModelFactory factory = new UmlStateMachineModelFactory("classpath:fsm/fsm.uml");
      // UmlStateMachineModelFactory factory = new UmlStateMachineModelFactory("classpath:fsm.simple/fsm.uml");
      return factory;
    }
  }

  @Configuration
  public class SchemeBeans {
    // ------------------------------------------------------------------------------------------
    // actions
    // TODO: Remove after debug
    @Bean
    DebugAction debugAction() {
      return new DebugAction();
    }

    @Bean
    CreateProduct createProduct() {
      return new CreateProduct();
    }

    @Bean
    PendingDisconnectEntry pendingDisconnectEntry() {
      return new PendingDisconnectEntry();
    }

    @Bean
    DisconnectionEntry disconnectionEntry() {
      return new DisconnectionEntry();
    }

    @Bean
    PaymentStoppingEntry paymentStoppingEntry() {
      return new PaymentStoppingEntry();
    }

    @Bean
    PaidEntry paidEntry() {
      return new PaidEntry();
    }

    @Bean
    NotPaidEntry notPaidEntry() {
      return new NotPaidEntry();
    }

    @Bean
    WaitingPaymentEntry waitingPaymentEntry() {
      return new WaitingPaymentEntry();
    }

    @Bean
    WaitingPaymentExit waitingPaymentExit() {
      return new WaitingPaymentExit();
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
    PriceChangingExit priceChangingExit() {
      return new PriceChangingExit();
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
    SuspendedEntry suspendedEntry() {
      return new SuspendedEntry();
    }

    @Bean
    SuspendedExit suspendedExit() {
      return new SuspendedExit();
    }

    @Bean
    ResumingEntry resumingEntry() {
      return new ResumingEntry();
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

    @Bean
    Action<String, String> clearProductPrice() {
      return new Action<String, String>() {
        public void execute(StateContext<String, String> context) {
          Product product = context.getExtendedState().get("product", Product.class);
          log.info("Clear product price. productId: {}", product.getProductId());
          product.setProductPrices(null);
        }
      };
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
