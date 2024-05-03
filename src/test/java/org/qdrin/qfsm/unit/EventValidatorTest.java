package org.qdrin.qfsm.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.qdrin.qfsm.EventBuilder;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.exception.BadUserDataException;
import org.qdrin.qfsm.model.ClientInfo;
import org.qdrin.qfsm.model.Event;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.utils.EventValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventValidatorTest {

    private Helper helper = new Helper();
    
    @Test
    public void directActivateEventTest() throws Exception {
        ClientInfo clientInfo = new ClientInfo();
        Event event = new Event();

        RequestEventDto incomingEvent = new EventBuilder("activation_started", "bundleOffer1", "bundle1-price-active")
                        .componentIds("component1", "component2")
                        .build();
        List<ProductActivateRequestDto> items = incomingEvent.getProductOrderItems();

        BadUserDataException exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);},
            "BadUserDataException was expected");              
        assert(exc.getMessage().contains("eventType"));

        event.setEventType("activation_started");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("refId"));
        
        event.setRefId("some_ref_id");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("refIdType"));

        event.setRefIdType("order_id");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("eventDate"));

        event.setEventDate(OffsetDateTime.now());
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("clientInfo"));

        event.setClientInfo(clientInfo);
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("partyId"));
        assert(exc.getMessage().contains("partyRoleId"));

        clientInfo.setPartyId("partyId");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("partyId"));
        assert(exc.getMessage().contains("partyRoleId"));

        clientInfo.setPartyRoleId("partyRoleId");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("productOrderItems"));

        event.setProductOrderItems(items);
        assertDoesNotThrow(() -> {EventValidator.validate(event);});
    }

    @Test
    public void directDefaultEventTest() throws Exception {
        Event event = new Event();
        List<ProductRequestDto> items = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            ProductRequestDto item = new ProductRequestDto();
            item.setProductId(String.format("product%d", i));
            items.add(item);
        }
        
        BadUserDataException exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);},
            "BadUserDataException was expected");              
        assert(exc.getMessage().contains("eventType"));

        event.setEventType("activation_completed");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("refId"));
        
        event.setRefId("some_ref_id");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("refIdType"));

        event.setRefIdType("order_id");
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("eventDate"));

        event.setEventDate(OffsetDateTime.now());
        exc = assertThrows(BadUserDataException.class, () -> {EventValidator.validate(event);}, "BadUserDataException was expected");              
        assert(exc.getMessage().contains("products"));

        event.setProducts(items);
        assertDoesNotThrow(() -> {EventValidator.validate(event);});
    }
}
