package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class ProductBuilder {
    String productId = null;
    String partyRoleId = UUID.randomUUID().toString();
    String productOfferingId = null;
    String productOfferingName = null;
    String productOfferingVersion = null;
    String productSpecificationId = null;
    String productSpecificationVersion = null;
    Boolean isBundle = false;
    Boolean isCustom = false;
    String status = null;
    String machineState = null;
    int productClass = 1;
    int tarificationPeriod = 0;
    OffsetDateTime trialEndDate = null;
    OffsetDateTime activeEndDate = null;
    OffsetDateTime productStartDate = null;
    List<ProductPrice> productPrice = null;
    List<ProductRelationship> productRelationship = null;
    List<FabricRef> fabricRef = null;
    List<ProductCharacteristic> characteristic = null;
    List<Characteristic> label = null;
    Map<String, Object> metaInfo = null;
    Map<String, Object> quantity = null;

    String priceId = null;

    private void recalc() {
        productId = UUID.randomUUID().toString();
        status = status == null ? "PENDING_ACTIVATE" : status;
        OfferDef offer = new Helper().getTestOffers().getOffers().get(this.productOfferingId);
        productOfferingName = offer.getName();
        Map<String, ProductPrice> prices = offer.getPrices();
        ProductPrice price = prices != null ? prices.get(priceId) : null;
        if(price != null) {
            price.setId(priceId);
        }
        this.productPrice = price != null ? Arrays.asList(price) : null;
        this.isBundle = offer.getIsBundle() == null ? this.isBundle : offer.getIsBundle();
        this.isCustom = offer.getIsCustom() == null ? this.isCustom : offer.getIsCustom();
        ProductClasses pclass = ProductClasses.SIMPLE;
        if(isBundle) {
            pclass = this.isCustom ? ProductClasses.CUSTOM_BUNDLE : ProductClasses.BUNDLE;
        } else if(prices == null) {
            pclass = this.isCustom ? ProductClasses.CUSTOM_BUNDLE_COMPONENT : ProductClasses.BUNDLE_COMPONENT;
        }
        this.productClass = pclass.ordinal();
    }

    public ProductBuilder(String offerId, String status, String priceId) {
        this.productOfferingId = offerId;
        this.priceId = priceId;
        this.status = status;
        recalc();
    }

    public ProductBuilder productId(String val) {productId = val; return this;}
    public ProductBuilder partyRoleId(String val) {partyRoleId = val; return this;}
    public ProductBuilder productOfferingId(String val) {productOfferingId = val; return this;}
    public ProductBuilder productOfferingName(String val) {productOfferingName = val; return this;}
    public ProductBuilder productOfferingVersion(String val) {productOfferingVersion = val; return this;}
    public ProductBuilder productSpecificationId(String val) {productSpecificationId = val; return this;}
    public ProductBuilder productSpecificationVersion(String val) {productSpecificationVersion = val; return this;}
    public ProductBuilder isBundle(Boolean val) {isBundle = val; recalc(); return this;}
    public ProductBuilder isCustom(Boolean val) {isCustom = val; recalc(); return this;}
    public ProductBuilder status(String val) {status = val; return this;}
    public ProductBuilder machineState(String val) {machineState = val; return this;}
    public ProductBuilder productClass(int val) {productClass = val; return this;}
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
    public ProductBuilder productRelationship(List<ProductRelationship> val) {productRelationship = val; return this;}
    public ProductBuilder fabricRef(List<FabricRef> val) {fabricRef = val; return this;}
    public ProductBuilder characteristic(List<ProductCharacteristic> val) {characteristic = val; return this;}
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
        product.setProductClass(productClass);
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
