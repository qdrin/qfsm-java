package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.EventDto;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class EventBuilder {
    private final Map<String, OfferDef> offers =Helper.testOffers.getOffers();
    private int mainIndex = 0;
    String refId = null;
    String refIdType = "orderId";
    String sourceCode = "CRAB_ML";
    String eventType = null;
    OffsetDateTime eventDate = null;
    String partyRoleId = null;
    String partyId = null;
    String productOfferingId = null;
    String bundleProductId = null;
    String priceId = null;
    List<String> componentIds = new ArrayList<>();
    Product product = null;
    List<Product> components = null;
    List<ProductRequestDto> products = null;
    List<ProductActivateRequestDto> productOrderItems = null;
    List<Characteristic> characteristics = null;
    EventProperties eventProperties = null;

    private void recalc() {
        refId = UUID.randomUUID().toString();
        eventDate = eventDate == null ? OffsetDateTime.now() : eventDate;
        if(eventType == "activation_started") {
            partyRoleId = UUID.randomUUID().toString();
            partyId = UUID.randomUUID().toString();
            products = null;
            productOrderItems = new ArrayList<>();
            ProductActivateRequestDto main = toMainProductActivateRequestDto(productOfferingId, priceId);
            if(main.getIsBundle()) {
                List<ProductOrderItemRelationshipDto> relations = new ArrayList<>();
                main.setProductOrderItemRelationship(relations);
                for(String componentId: componentIds) {
                    ProductActivateRequestDto item = toComponentProductActivateRequestDto(componentId, main.getProductOrderItemId());
                    ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
                    rel.setProductOrderItemId(item.getProductOrderItemId());
                    String relationType = main.getIsCustom() ? "CUSTOM_BUNDLES" : "BUNDLES";
                    rel.setRelationshipType(relationType);
                    relations.add(rel);
                    productOrderItems.add(item);
                }
            } else {  // "CUSTOM_BUNDLE component"
                List<ProductOrderItemRelationshipDto> relations = new ArrayList<>();
                ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
                rel.setProductId(bundleProductId);
                rel.setRelationshipType("BELONGS");
                main.setProductOrderItemRelationship(relations);
            }
            productOrderItems.add(mainIndex, main);
        } else {
            partyRoleId = null;
            partyId = null;
            productOrderItems = null;
            products = new ArrayList<>();
            ProductRequestDto main = new ProductRequestDto();
            main.setProductId(product.getProductId());
            products.add(mainIndex, main);
            ArrayList<ProductRelationship> relations = new ArrayList<>();
            main.setProductRelationship(relations);
            String reltype;
            switch(ProductClass.values()[product.getProductClass()]) {
                case BUNDLE:
                    reltype = "BUNDLES"; break;
                case CUSTOM_BUNDLE:
                    reltype = "CUSTOM_BUNDLES"; break;
                default:
                    reltype = "";
            }
            components.stream().forEach(c -> {
                ProductRequestDto cRequestDto = new ProductRequestDto();
                cRequestDto.setProductId(c.getProductId());
                ProductRelationship r = new ProductRelationship();
                r.setProductId(c.getProductId());
                r.setRelationshipType(reltype);
                relations.add(r);
                products.add(cRequestDto);
            });
        }
    }

    private ProductActivateRequestDto toMainProductActivateRequestDto(String offerId, String priceId) {
        OfferDef offerDef = offers.get(productOfferingId);
        ProductActivateRequestDto orderItem = new ProductActivateRequestDto();
        orderItem.setProductOrderItemId(UUID.randomUUID().toString());
        orderItem.setProductOfferingId(offerId);
        orderItem.setProductOfferingName(offerDef.getName());
        orderItem.setFabricRef(offerDef.getFabricRef());
        orderItem.setIsBundle(offerDef.getIsBundle());
        orderItem.setIsCustom(offerDef.getIsCustom());
        if(priceId != null) {
            ProductPrice price = offerDef.getPrices().get(priceId);
            price.setId(priceId);
            orderItem.setProductPrice(Arrays.asList(price));
        }
        return orderItem;
    }

    private ProductActivateRequestDto toComponentProductActivateRequestDto(String offerId, String mainId) {
        OfferDef offerDef = offers.get(offerId);
        ProductActivateRequestDto orderItem = new ProductActivateRequestDto();
        orderItem.setProductOrderItemId(UUID.randomUUID().toString());
        if(mainId != null) {
            ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
            rel.setRelationshipType("CUSTOM_BUNDLES");
            rel.setProductOrderItemId(mainId);
        }
        orderItem.setProductOfferingId(offerId);
        orderItem.setProductOfferingName(offerDef.getName());
        orderItem.setFabricRef(offerDef.getFabricRef());
        orderItem.setIsBundle(false);
        orderItem.setIsCustom(false);
        return orderItem;
    }

    public EventBuilder(String eventType, Product product) {
        this.eventType = eventType;
        this.product = product;
        this.components = new ArrayList<>();
        recalc();
    }

    public EventBuilder(String eventType, TestBundle testBundle) {
        this.eventType = eventType;
        this.product = testBundle.drive;
        this.components = testBundle.components();
        recalc();
    }

    public EventBuilder(String eventType, String mainOfferId, String priceId) {
        this.eventType = eventType;
        this.productOfferingId = mainOfferId;
        this.priceId = priceId;
        this.components = new ArrayList<>();
        recalc();
    }

    public EventBuilder product(Product val) {product = val; return this;}
    public EventBuilder partyRoleId(String val) {partyRoleId = val; return this;}
    public EventBuilder partyId(String val) {partyId = val; return this;}
    public EventBuilder sourceCode(String val) {sourceCode = val; return this;}
    public EventBuilder refId(String val) {refId = val; return this;}
    public EventBuilder eventType(String val) {eventType = val; return this;}
    public EventBuilder eventDate(OffsetDateTime val) {eventDate = val; return this;}
    public EventBuilder refIdType(String val) {refIdType = val; return this;}
    public EventBuilder productOfferingId(String val) {productOfferingId = val; return this;}
    public EventBuilder priceId(String val) {priceId = val; return this;}
    public EventBuilder bundleProductId(String val) {bundleProductId = val; return this;}
    public EventBuilder mainIndex(int val) {mainIndex = val; return this;}
    public EventBuilder characteristics(List<Characteristic> val) {characteristics = val; return this;}
    public EventBuilder eventProperties(EventProperties val) {eventProperties = val; return this;}
    public EventBuilder componentIds(String... componentIds) {
        this.componentIds.clear();
        for(String id: componentIds) {
            this.componentIds.add(id);
        }
        recalc();
        return this;
    }

    public RequestEventDto build() {
        recalc();
        RequestEventDto event = new RequestEventDto();
        EventDto eventDto = new EventDto();
        eventDto.setEventDate(eventDate);
        eventDto.setEventType(eventType);
        eventDto.setRefId(refId);
        eventDto.setRefIdType(refIdType);
        eventDto.setSourceCode(sourceCode);
        event.setEvent(eventDto);

        event.setCharacteristics(characteristics);
        event.setEventProperties(eventProperties);
        if(eventType.equals("activation_started")) {
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setPartyId(partyId);
            clientInfo.setPartyRoleId(partyRoleId);
            event.setClientInfo(clientInfo);
            event.setProductOrderItems(productOrderItems);
        } else {
            event.setProducts(products);
        }
        return event;
    }
}
