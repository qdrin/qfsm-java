package org.qdrin.qfsm.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.qdrin.qfsm.exception.BadUserDataException;
import org.qdrin.qfsm.model.ClientInfo;
import org.qdrin.qfsm.model.Event;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;

public class EventValidator {
  private static void checkCommonParameters(Event event) {
    OffsetDateTime eventDate = event.getEventDate();
    if(eventDate == null || eventDate.isAfter(OffsetDateTime.now())) {
      throw new BadUserDataException(String.format("Bad eventDate (null or future): %s", eventDate.toInstant()));
    }
  }
  private static void checkClientInfo(Event event) {
    ClientInfo clientInfo = event.getClientInfo();
    if(clientInfo == null) {
      throw new BadUserDataException(String.format("Empty clientInfo"));
    }
    String partyRoleId = clientInfo.getPartyRoleId();
    String partyId = clientInfo.getPartyId();
    if(partyRoleId == null || partyRoleId.isEmpty()
          || partyId == null || partyRoleId.isEmpty()) {
      throw new BadUserDataException(String.format("Mandatory attribute(s) is missing: partyId: %s, partyRoleId: %s", partyId, partyRoleId));
    }
    
  }          // check if clientInfo is present
  private static void checkProductOrderItems(Event event) {
    List<ProductActivateRequestDto> orderItems = event.getProductOrderItems();
    if(orderItems == null || orderItems.isEmpty()) {
      throw new BadUserDataException(String.format("Mandatory attribute(s) is missing. productOrderItems: %s", orderItems));
    }
    // Check for doubles
    for(ProductActivateRequestDto item: orderItems) {
      if(orderItems.stream().anyMatch(p -> p != item && p.getProductOrderItemId().equals(item.getProductOrderItemId()))) {
        throw new BadUserDataException(String.format("Double productOrderItemId found: %s", item.getProductOrderItemId()));
      }
    }
  }
  private static void checkProducts(Event event) {
    List<ProductRequestDto> orderItems = event.getProducts();
    if(orderItems == null || orderItems.isEmpty()) {
      throw new BadUserDataException(String.format("Mandatory attribute(s) is missing. products: %s", orderItems));
    }
    // Check for doubles
    for(ProductRequestDto item: orderItems) {
      if(orderItems.stream().anyMatch(p -> p != item && p.getProductId().equals(item.getProductId()))) {
        throw new BadUserDataException(String.format("Double productId found: %s", item.getProductId()));
      }
    }
  }

  private static void checkNextPrice(Event event) {
    
  }

  public static void validate(Event event) {
    String eventType = event.getEventType();
    checkCommonParameters(event);
    switch(eventType) {
      case "activation_started":
        checkClientInfo(event);
        checkProductOrderItems(event);
        break;
      case "change_price":
        checkNextPrice(event);
      default:
        checkProducts(event);
    }
  }
}
