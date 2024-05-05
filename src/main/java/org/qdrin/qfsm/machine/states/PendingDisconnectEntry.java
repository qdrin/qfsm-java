package org.qdrin.qfsm.machine.states;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.machine.actions.AddActionAction;
import org.qdrin.qfsm.machine.actions.DeleteActionAction;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.qdrin.qfsm.tasks.ExternalData;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class PendingDisconnectEntry implements Action<String, String> {

  @Override
  public void execute(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    log.debug("PendingActivateEntry started. event: {}, message: {}", context.getEvent());
    // TODO: Remove after manual mode
    List<ProductCharacteristic> chars = ExternalData.requestProductCharacteristics();
    if(chars.size() > 0) {
      product.setCharacteristic(chars);
      log.debug("product with characteristics: {}", product);
    }
    
    // ----------------------------------------------------------------------------------
    List<ProductCharacteristic> characteristics = product.getCharacteristic();
    Optional<ProductCharacteristic> ch = Optional.empty();
    if(characteristics != null) {
      ch = product.getCharacteristic().stream()
        .filter(c -> {return c.getRefName().equals("deactivation_mode");})
        .findFirst();
    }
    String productMode = ch.isPresent() ? ch.get().getValue().toString() : "Postponed";
    log.debug("deactivation mode: {}", productMode);


    Mono<Message<String>> paymentOff = Mono.just(MessageBuilder
      .withPayload("payment_off").build());
    Mono<Message<String>> priceOff = Mono.just(MessageBuilder
      .withPayload("price_off").build());
    var paymentRes = context.getStateMachine().sendEvent(paymentOff).collectList();
    var priceRes = context.getStateMachine().sendEvent(priceOff).collectList();
    new DeleteActionAction(
      ActionSuit.PRICE_ENDED,
      ActionSuit.SUSPEND_ENDED,
      ActionSuit.WAITING_PAY_ENDED,
      ActionSuit.CHANGE_PRICE,
      ActionSuit.RESUME_EXTERNAL
      ).execute(context);
    new AddActionAction(ActionSuit.DISCONNECT).execute(context);  // product.getActiveEndDate().toInstant() or characteristic-valued
    paymentRes.block();
    priceRes.block();
  }
}
