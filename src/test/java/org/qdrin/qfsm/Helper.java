package org.qdrin.qfsm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.ClientInfo;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.dto.EventDto;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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

  final TestOffers testOffers = setTestOffers();

  private TestOffers setTestOffers() {
    ClassPathResource resource = new ClassPathResource("/offers.yaml", getClass());
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                              // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    TestOffers offers = null;
    try {
      offers = yamlMapper.readValue(resource.getInputStream(), TestOffers.class);
    } catch (StreamReadException e) {
      e.printStackTrace();
    } catch (DatabindException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return offers;
  }

  public TestOffers getTestOffers() {
    return testOffers;
  }

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
      public List<ProductRequestDto> products = null;
      public List<ProductActivateRequestDto> productOrderItems = null;
      public List<Characteristic> characteristics = null;
      public EventProperties eventProperties = null;

      public Builder() {}

      public Builder eventType(String val) { 
        eventType = val;
        return this;
      }

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

  public List<ProductActivateRequestDto> buildOrderItems(
        String mainOfferId,
        String priceId,
        String... componentOffers) {
    Map<String, OfferDef> offers = getTestOffers().getOffers();
    log.debug("componentOffers: {}", componentOffers);
    ArrayList<ProductActivateRequestDto> items = new ArrayList<>();
    ProductActivateRequestDto bundle = new ProductActivateRequestDto();
    String bundleItemId = UUID.randomUUID().toString();
    List<ProductOrderItemRelationshipDto> relations = new ArrayList<>();
    OfferDef bundleOffer = offers.get(mainOfferId);
    ProductPrice price = bundleOffer.getPrices().get(priceId);
    price.setId(priceId);
    String relationType = bundleOffer.getIsCustom() ? "CUSTOM_BUNDLES" : "BUNDLES";
    bundle.setProductOrderItemId(bundleItemId);
    bundle.setProductOfferingId(mainOfferId);
    bundle.setProductOfferingName(bundleOffer.getName());
    bundle.setFabricRef(bundleOffer.getFabricRef());
    bundle.setIsBundle(bundleOffer.getIsBundle());
    bundle.setIsCustom(bundleOffer.getIsCustom());
    bundle.setProductPrice(Arrays.asList(price));
    items.add(bundle);
    if(componentOffers != null) {
      bundle.setProductOrderItemRelationship(relations);
      for(int i = 0; i < componentOffers.length; i++) {
        OfferDef componentOffer = offers.get(componentOffers[i]);
        ProductActivateRequestDto component = new ProductActivateRequestDto();
        String componentItemId = UUID.randomUUID().toString();
        component.setProductOrderItemId(componentItemId);
        component.setProductOfferingName(componentOffer.getName());
        component.setIsBundle(false);
        component.setIsCustom(false);
        component.setProductOfferingId(componentOffers[i]);
        ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
        rel.setProductOrderItemId(componentItemId);
        rel.setRelationshipType(relationType);  // TODO: CUSTOM_BUNDLES???
        relations.add(rel);
        items.add(component);
      }
    }
    return items;
  }
}
