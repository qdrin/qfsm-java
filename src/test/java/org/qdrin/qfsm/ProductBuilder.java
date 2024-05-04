package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import com.fasterxml.jackson.databind.*;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PUBLIC)
public class ProductBuilder {
    String productId = UUID.randomUUID().toString();
    String partyRoleId = UUID.randomUUID().toString();
    String productOfferingId = null;
    String productOfferingName = null;
    String productOfferingVersion = null;
    String productSpecificationId = null;
    String productSpecificationVersion = null;
    Boolean isBundle = false;
    Boolean isCustom = false;
    String status = null;
    JsonNode machineState = new ObjectMapper().createObjectNode();
    ProductClass productClass = null;
    int tarificationPeriod = -1;
    OffsetDateTime trialEndDate = null;
    OffsetDateTime activeEndDate = null;
    OffsetDateTime productStartDate = OffsetDateTime.now();
    List<ProductPrice> productPrice = new ArrayList<>();
    List<ProductRelationship> productRelationship = new ArrayList<>();
    List<FabricRef> fabricRef = new ArrayList<>();
    List<ProductCharacteristic> characteristic = new ArrayList<>();
    List<Characteristic> label = new ArrayList<>();
    Map<String, Object> metaInfo = new HashMap<>();
    Map<String, Object> quantity = new HashMap<>();

    String priceId = null;

    private void clear() {
        productOfferingId = null;
        productOfferingName = null;
        productOfferingVersion = null;
        productSpecificationId = null;
        productSpecificationVersion = null;
        isBundle = false;
        isCustom = false;
        status = null;
        machineState = new ObjectMapper().createObjectNode();
        productClass = null;
        tarificationPeriod = -1;
        trialEndDate = null;
        activeEndDate = null;
        productStartDate = null;
        productPrice.clear();
        productRelationship.clear();
        fabricRef.clear();
        characteristic.clear();
        label.clear();
        metaInfo.clear();
        quantity.clear();
    }

    private void recalc() {
        status = status == null ? "PENDING_ACTIVATE" : status;
        OfferDef offer = Helper.testOffers.getOffers().get(this.productOfferingId);
        productOfferingName = offer.getName();
        Map<String, ProductPrice> prices = offer.getPrices();
        ProductPrice price = prices != null ? prices.get(priceId) : null;
        if(price != null) {
            price.setId(priceId);
            this.productPrice.add(price);
        }
        this.isBundle = offer.getIsBundle() == null ? this.isBundle : offer.getIsBundle();
        this.isCustom = offer.getIsCustom() == null ? this.isCustom : offer.getIsCustom();
        List<ProductClass> pclasses = offer.getProductClass();
        if(productClass == null && pclasses.size() == 1) {
            this.productClass = pclasses.get(0);
        }
    }

    public ProductBuilder(String offerId, String status, String priceId) {
        this.productOfferingId = offerId;
        this.priceId = priceId;
        this.status = status;
        recalc();
    }

    public ProductBuilder(ProductActivateRequestDto orderItem) {
        this();
        this.productOfferingId = orderItem.getProductOfferingId();
        this.productOfferingName = orderItem.getProductOfferingName();
        this.isBundle = orderItem.getIsBundle();
        this.isCustom = orderItem.getIsCustom();
        if(orderItem.getProductPrice() != null) { this.productPrice = orderItem.getProductPrice(); }
        if(orderItem.getCharacteristic() != null) { this.characteristic = orderItem.getCharacteristic(); }
        if(orderItem.getFabricRef() != null) { this.fabricRef = orderItem.getFabricRef(); }
        if(orderItem.getMetaInfo() != null) { this.metaInfo = orderItem.getMetaInfo(); }
        if(orderItem.getLabel() != null) { this.label = orderItem.getLabel(); }
        this.status = "PENDING_ACTIVATE";
        recalc();
    }

    public ProductBuilder(Product product) {
        this();
        this.productId = product.getProductId();
        this.partyRoleId = product.getPartyRoleId();
        this.productOfferingId = product.getProductOfferingId();
        this.productOfferingName = product.getProductOfferingName();
        this.productSpecificationId = product.getProductSpecificationId();
        this.productSpecificationVersion = product.getProductSpecificationVersion();
        this.isBundle = product.getIsBundle();
        this.isCustom = product.getIsCustom();
        this.status = product.getStatus();
        this.productClass = ProductClass.values()[product.getProductClass()];
        this.tarificationPeriod = product.getTarificationPeriod();
        this.productStartDate = product.getProductStartDate();
        this.activeEndDate = product.getActiveEndDate();
        this. trialEndDate = product.getTrialEndDate();
        if(product.getProductPrice() != null) { this.productPrice = product.getProductPrice(); }
        if(product.getProductRelationship() != null) { this.productRelationship = product.getProductRelationship(); }
        if(product.getCharacteristic() != null) { this.characteristic = product.getCharacteristic(); }
        if(product.getFabricRef() != null) { this.fabricRef = product.getFabricRef(); }
        if(product.getMetaInfo() != null) { this.metaInfo = product.getMetaInfo(); }
        if(product.getLabel() != null) { this.label = product.getLabel(); }
        if(product.getQuantity() != null) { this.quantity = product.getQuantity(); }
        if(product.getMachineState() != null) { this.machineState = product.getMachineState(); }

        priceId = product.getProductPrice(PriceType.RecurringCharge).get().getId();
        recalc();
    }

    public ProductBuilder productId(String val) {productId = val; return this;}
    public ProductBuilder partyRoleId(String val) {partyRoleId = val; return this;}
    public ProductBuilder productOfferingId(String val) {
        clear();
        productOfferingId = val;
        recalc();
        return this;
    }
    public ProductBuilder productOfferingName(String val) {productOfferingName = val; return this;}
    public ProductBuilder productOfferingVersion(String val) {productOfferingVersion = val; return this;}
    public ProductBuilder productSpecificationId(String val) {productSpecificationId = val; return this;}
    public ProductBuilder productSpecificationVersion(String val) {productSpecificationVersion = val; return this;}
    public ProductBuilder isBundle(Boolean val) {isBundle = val; recalc(); return this;}
    public ProductBuilder isCustom(Boolean val) {isCustom = val; recalc(); return this;}
    public ProductBuilder status(String val) {status = val; return this;} 
    public ProductBuilder machineState(JsonNode val) {machineState = val; return this;}
    public ProductBuilder productClass(ProductClass val) {productClass = val; return this;}
    public ProductBuilder tarificationPeriod(int val) {tarificationPeriod = val; return this;}
    public ProductBuilder trialEndDate(OffsetDateTime val) {trialEndDate = val; return this;}
    public ProductBuilder activeEndDate(OffsetDateTime val) {activeEndDate = val; return this;}
    public ProductBuilder productStartDate(OffsetDateTime val) {productStartDate = val; return this;}
    public ProductBuilder productPrice(List<ProductPrice> val) {
        productPrice = val;
        this.priceId = (val != null && ! val.isEmpty()) ? val.get(0).getId() : null;
        recalc();
        return this;
    }
    public ProductBuilder priceId(String val) {this.priceId = val; recalc(); return this; }
    public ProductBuilder pricePeriod(int val) {this.productPrice.get(0).setPeriod(val); return this; }
    public ProductBuilder productRelationship(List<ProductRelationship> val) {productRelationship = val; return this;}
    public ProductBuilder fabricRef(List<FabricRef> val) {fabricRef = val; return this;}
    public ProductBuilder characteristic(List<ProductCharacteristic> val) {
        if(val == null) {
            characteristic.clear();
        } else {
            characteristic = val;
        }
        return this;
    }
    public ProductBuilder label(List<Characteristic> val) {label = val; return this;}
    public ProductBuilder metaInfo(Map<String, Object> val) {metaInfo = val; return this;}
    public ProductBuilder quantity(Map<String, Object> val) {quantity = val; return this;}

    public Product build() {
        Product product = new Product();
        product.setProductId(productId);
        product.setPartyRoleId(partyRoleId);
        product.setProductOfferingId(productOfferingId);
        product.setProductOfferingName(productOfferingName);
        product.setProductOfferingVersion(productOfferingVersion);
        product.setProductSpecificationId(productSpecificationId);
        product.setProductSpecificationVersion(productSpecificationVersion);
        product.setIsBundle(isBundle);
        product.setIsCustom(isCustom);
        product.setStatus(status);
        product.setMachineState(machineState);
        if(productClass != null) {
            product.setProductClass(productClass.ordinal());
        } else {
            product.setProductClass(-1);
        }
        product.setTarificationPeriod(tarificationPeriod);
        product.setTrialEndDate(trialEndDate);
        product.setActiveEndDate(activeEndDate);
        product.setProductStartDate(productStartDate);
        product.setProductPrice(productPrice);
        product.setProductRelationship(productRelationship);
        product.setFabricRef(fabricRef);
        product.setCharacteristic(characteristic);
        product.setLabel(label);
        product.setMetaInfo(metaInfo);
        product.setQuantity(quantity);
        
        return product;
    }
}
