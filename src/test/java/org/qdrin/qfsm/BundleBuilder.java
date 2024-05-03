package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

// @FieldDefaults(level = AccessLevel.PUBLIC)
public class BundleBuilder {
    final static List<Integer> mainClasses = Arrays.asList(
        ProductClass.SIMPLE.ordinal(), ProductClass.BUNDLE.ordinal(), ProductClass.CUSTOM_BUNDLE.ordinal());

    public class TestBundle {
        public Product bundle;
        public List<Product> components;

        public Product getByOfferId(String offerId) {
            return products().stream().filter(p -> p.getProductOfferingId().equals(offerId)).findFirst().get();
        }

        public List<Product> products() {
            List<Product> res = new ArrayList<>();
            res.add(bundle);
            for(Product component: components) {
                res.add(component);
            }
            return res;
        }
    }
    @Autowired
    ProductRepository productRepository;

    String partyRoleId = UUID.randomUUID().toString();
    String eventType = null;
    ProductClass componentClass = null;
    ProductClass bundleClass = null;

    Product bundle = null;
    List<Product> components = new ArrayList<>();

    private void setComponentClass(ProductClass cclass) {
        if(components != null) {
            for(Product component: components) {
                component.setProductClass(cclass.ordinal());     
            }
        }
    }

    private void createFromProducts(List<Product> products) {
        for(Product product: products) {
            if(mainClasses.contains(product.getProductClass())) {
                assert(bundle == null);
                bundle = product;
                bundleClass = ProductClass.values()[product.getProductClass()];
                switch(bundleClass) {
                    case BUNDLE:
                        componentClass = ProductClass.BUNDLE_COMPONENT;
                        break;
                    case CUSTOM_BUNDLE:
                        componentClass = ProductClass.CUSTOM_BUNDLE_COMPONENT;
                        break;
                    default:
                        componentClass = null;
                }
            } else {
                components.add(product);
            }
        }
        if(componentClass != null) {
            setComponentClass(componentClass);
        }
    }

    public BundleBuilder(List<Product> products) {
        createFromProducts(products);
    }

    public BundleBuilder(RequestEventDto event) {
        String eventType = event.getEvent().getEventType();
        this.eventType = eventType;
        List<Product> products = new ArrayList<>();
        if(eventType.equals("activation_started")) {
            this.partyRoleId = event.getClientInfo().getPartyRoleId();
            for(ProductActivateRequestDto item: event.getProductOrderItems()) {
                Product product = new ProductBuilder(item)
                        .productId(null)
                        .partyRoleId(partyRoleId)
                        .build();
                products.add(product);
            }
        } else {  // Обработка все событий, кроме activation_started
            for(ProductRequestDto item: event.getProducts()) {
                Product product = productRepository.findById(item.getProductId()).get();
                assert(product != null);
                products.add(product);
            }        
        }
        createFromProducts(products);
    }

    public BundleBuilder componentClass(ProductClass componentClass) {
        this.componentClass = componentClass;
        setComponentClass(componentClass);
        return this;
    }

    public BundleBuilder bundleClass(ProductClass bundleClass) {
        bundle.setProductClass(bundleClass.ordinal());
        return this;
    }

    public BundleBuilder addComponent(Product component) {
        components.add(component);
        ProductRelationship relation = new ProductRelationship();
        relation.setProductId(component.getProductId());
        List<ProductRelationship> relations = bundle.getProductRelationship();
        relations = relations == null ? new ArrayList<>() : relations;
        relations.add(relation);
        bundle.setProductRelationship(relations);
        return this;
    }

    public BundleBuilder addComponent(String offerId) {
        String status = bundle.getStatus();
        Product product = new ProductBuilder(offerId, status, null)
            .productId(null)
            .partyRoleId(partyRoleId)
            .productClass(componentClass)
            .build();
        components.add(product);
        return this;
    }

    public BundleBuilder removeComponent(String productId) {
        components.stream()
            .dropWhile(p -> p.getProductId().equals(productId));
        bundle.getProductRelationship().stream()
            .dropWhile(p -> p.getProductId().equals(productId));
        return this;
    }

    public BundleBuilder productIds(List<Product> products) {
        for(Product product: products) {
            if(bundle.getProductOfferingId().equals(product.getProductOfferingId())) {
                bundle.setProductId(product.getProductId());
            } else {
                for(Product component: components) {
                    if(component.getProductOfferingId().equals(product.getProductOfferingId())) {
                        component.setProductId(product.getProductId());
                        continue;
                    }
                }
            }
        }
        return this;
    }

    public BundleBuilder tarificationPeriod(int tarificationPeriod) {
        bundle.setTarificationPeriod(tarificationPeriod);
        for(Product component: components) {
            component.setTarificationPeriod(tarificationPeriod);
        }
        return this;
    }

    public TestBundle build() {
        TestBundle testBundle = new TestBundle();
        testBundle.bundle = this.bundle;
        testBundle.components = this.components;
        return testBundle;
    }
}
