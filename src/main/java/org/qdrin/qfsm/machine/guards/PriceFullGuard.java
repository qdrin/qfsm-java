package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.*;

@Slf4j
public class PriceFullGuard implements Guard<String, String> {

  boolean notFull;

  public PriceFullGuard(boolean notFull) {
    this.notFull = notFull;
  }

  public PriceFullGuard() {
    this(false);
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    ProductPrice price =  extendedState.get("product", Product.class).getProductPrice(PriceType.RecurringCharge).get();
    String priceId = price == null ? "" : price.getId();
    
    List<Characteristic> eventChars = (List<Characteristic>) context.getMessageHeader("characteristics");
    eventChars = eventChars == null ? new ArrayList<>() : eventChars;
    List<Characteristic> nextPrices = eventChars.stream().filter(c -> c.getName().equals("nextPrice")).toList();
    if(nextPrices.isEmpty()) {
      log.error("No nextPrice characteristic found");
      return notFull;
    }
    ProductPrice nextPrice = (ProductPrice) nextPrices.get(0).getValue();
    OffsetDateTime nextPayDate = nextPrice.getNextPayDate();
    if(nextPayDate == null || nextPayDate.isBefore(OffsetDateTime.now())) return notFull;
    return ! notFull;
  }
  
}

