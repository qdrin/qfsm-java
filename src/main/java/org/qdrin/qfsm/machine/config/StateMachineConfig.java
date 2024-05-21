package org.qdrin.qfsm.machine.config;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.machine.actions.MergeComponent;
import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.machine.guards.*;
import org.qdrin.qfsm.machine.states.*;
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.service.QStateMachineContextConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.uml.UmlStateMachineModelFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StateMachineConfig {
  
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
    PendingActivateEntry pendingActivateEntry() {
      return new PendingActivateEntry();
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
    PriceChangedExit priceChangedExit() {
      return new PriceChangedExit();
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
    SignalAction sendPaymentOffAndPriceOff() {
      return new SignalAction("payment_off", "price_off");
    }

    @Bean
    Action<String, String> createNextPriceVariable() {
      return new Action<String, String>() {
        public void execute(StateContext<String, String> context) {
          ExtendedState extendedState = context.getExtendedState();
          ProductPrice nextPrice = null;
          List<Characteristic> eventChars = (List<Characteristic>) context.getMessageHeader("characteristics");
          if(eventChars != null) {
            Optional<Characteristic> nextPriceChar = eventChars.stream().filter(c -> c.getName().equals("nextPrice")).findFirst();
            if(nextPriceChar.isPresent()) {
              nextPrice = (ProductPrice) nextPriceChar.get().getValue();
            }
          }
          log.info("[{}] setting nextPrice: {}", extendedState.get("product", Product.class).getProductId(), nextPrice);
          extendedState.getVariables().put("nextPrice", nextPrice);
        }
      };
    }

    @Bean
    MergeComponent mergeComponent() {
      return new MergeComponent();
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
      return new PriceFullGuard(true);
    }

    @Bean
    public Guard<String, String> fullPrice() {
      return new PriceFullGuard(false);
    }

    @Bean
    public Guard<String, String> newPrice() {
      return new PriceEqualsGuard(true);
    }

    @Bean
    public Guard<String, String> samePrice() {
      return new PriceEqualsGuard(false);
    }

    @Bean
    public Guard<String, String> isTarificated() {
      return new ProductClassGuard(Arrays.asList(
        ProductClass.SIMPLE, ProductClass.BUNDLE, ProductClass.CUSTOM_BUNDLE
      ));
    }

    @Bean
    public Guard<String, String> isNotTarificated() {
      return new ProductClassGuard(Arrays.asList(
        ProductClass.BUNDLE_COMPONENT, ProductClass.CUSTOM_BUNDLE_COMPONENT
      ));
    }

    @Bean
    public Guard<String, String> canActivate() {
      return new CanActivateGuard();
    }

    @Bean
    public Guard<String, String> canDisconnect() {
      return new ProductClassGuard(Arrays.asList(
        ProductClass.SIMPLE, ProductClass.BUNDLE, ProductClass.CUSTOM_BUNDLE, ProductClass.CUSTOM_BUNDLE_COMPONENT
      ));
    }

    @Bean
    public Guard<String, String> activeEndDateInFuture() {
      return new Guard<String, String>() {
        @Override
        public boolean evaluate(StateContext<String, String> context) {
          Product product = context.getExtendedState().get("product", Product.class);
          return product.getActiveEndDate().isAfter(OffsetDateTime.now());
        }
      };
    }
  }
}
