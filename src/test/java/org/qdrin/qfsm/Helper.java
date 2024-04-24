package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.ClientInfo;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.dto.EventDto;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Helper {
  
  @Autowired
  ProductRepository productRepository;

  @Autowired
  EventRepository eventRepository;

  @Autowired
  ContextRepository contextRepository;

  public void clearDb() {
    productRepository.deleteAll();
    contextRepository.deleteAll();
    eventRepository.deleteAll();
  }

  public static class TestEvent {
    public final RequestEventDto requestEvent;

    public static class Builder {
      public String eventType = "activation_started";
      public String sourceCode = "OMS";
      public String refId = UUID.randomUUID().toString();
      public String refIdType = "orderId";
      public String partyRoleId = UUID.randomUUID().toString();
      public String partyId = UUID.randomUUID().toString();
      public List<ProductRequestDto> products;
      public List<ProductActivateRequestDto> productOrderItems;
      public List<Characteristic> characteristics;
      public EventProperties eventProperties;

      public Builder() {}

      public Builder sourceCode(String val) { 
        sourceCode = val;
        return this;
      }
      public Builder refId(String val) {
        refId = val;
        return this;
      }
      public Builder refIdType(String val) {
        refIdType = val;
        return this;
      }
      public Builder partyRoleId(String val) {
        partyRoleId = val;
        return this;
      }
      public Builder partyId(String val) {
        partyId = val;
        return this;
      }
      public Builder product(ProductRequestDto product) {
        if(products == null) {
          products = new ArrayList<>();
        }
        products.add(product);
        return this;
      }
      public Builder products(List<ProductRequestDto> products) {
        this.products = products;
        return this;
      }
      public Builder productOrderItem(ProductActivateRequestDto item) {
        if(productOrderItems == null) {
          productOrderItems = new ArrayList<>();
        }
        productOrderItems.add(item);
        return this;
      }
      public Builder productOrderItems(List<ProductActivateRequestDto> items) {
        this.productOrderItems = items;
        return this;
      }
      public Builder characteristics(List<Characteristic> chars) {
        this.characteristics = chars;
        return this;
      }
      public Builder eventProperties(EventProperties props) {
        eventProperties = props;
        return this;
      }

      public TestEvent build() {
        return new TestEvent(this);
      }
    }

    private TestEvent(Builder builder) {
      requestEvent = new RequestEventDto();
      EventDto event = new EventDto();
      event.setEventType(builder.eventType);
      event.setRefId(builder.refId);
      event.setRefIdType(builder.refIdType);
      event.setSourceCode(builder.sourceCode);
      requestEvent.setEvent(event);
      requestEvent.setCharacteristics(builder.characteristics);
      requestEvent.setEventProperties(builder.eventProperties);
      switch(builder.eventType) {
        case "activation_started":
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setPartyId(builder.partyId);
            clientInfo.setPartyRoleId(builder.partyRoleId);
            requestEvent.setClientInfo(clientInfo);
            requestEvent.setProductOrderItems(builder.productOrderItems);
            break;
        default:
            requestEvent.setProducts(builder.products);
      }
    }
  }

  public static List<ProductActivateRequestDto> buildOrderItems(
        String mainOfferId,
        String priceId,
        String... componentOffers) {
    log.debug("componentOffers.length(): {}", componentOffers.length);
    ArrayList<ProductActivateRequestDto> items = new ArrayList<>();
    ProductActivateRequestDto bundle = new ProductActivateRequestDto();
    String bundleItemId = UUID.randomUUID().toString();
    List<ProductOrderItemRelationshipDto> relations = new ArrayList<>();
    bundle.setProductOrderItemId(bundleItemId);
    bundle.setProductOfferingId(mainOfferId);
    bundle.setProductOfferingName("");
    items.add(bundle);
    if(componentOffers.length > 0) {
      bundle.setProductOrderItemRelationship(relations);
    }
    for(int i = 0; i < componentOffers.length; i++) {
      ProductActivateRequestDto component = new ProductActivateRequestDto();
      String componentItemId = UUID.randomUUID().toString();
      component.setProductOrderItemId(componentItemId);
      component.setIsBundle(false);
      component.setIsCustom(false);
      component.setProductOfferingId(componentOffers[i]);
      ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
      rel.setProductOrderItemId(componentItemId);
      rel.setRelationshipType("BUNDLES");  // TODO: CUSTOM_BUNDLES???
      relations.add(rel);
      items.add(component);
    }
    return items;
  }
}
