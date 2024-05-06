package org.qdrin.qfsm;

import static org.mockito.ArgumentMatchers.nullable;

import java.time.OffsetDateTime;
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

import com.fasterxml.jackson.databind.JsonNode;

// @FieldDefaults(level = AccessLevel.PUBLIC)
public class BundleBuilder {
    final static List<Integer> mainClasses = Arrays.asList(
        ProductClass.SIMPLE.ordinal(), ProductClass.BUNDLE.ordinal(), ProductClass.CUSTOM_BUNDLE.ordinal());

    public class TestBundle {
        public Product drive;
        public Product bundle;
        public List<Product> components;

        public Product getByOfferId(String offerId) {
            return products().stream().filter(p -> p.getProductOfferingId().equals(offerId)).findFirst().get();
        }

        public List<Product> products() {
            List<Product> res = new ArrayList<>();
            if(drive != null) { res.add(drive); }
            if(bundle != null && ! bundle.getProductOfferingId().equals(drive.getProductOfferingId())) {
                res.add(bundle);
            }
            for(Product component: components) {
                if(! component.getProductOfferingId().equals(drive.getProductOfferingId())) {
                    res.add(component);
                }
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

    Product drive = null;   // Working product. Can be bundle or custom_bundle component or simple
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
            ProductClass pclass = ProductClass.values()[product.getProductClass()];
            switch(pclass) {
                case SIMPLE:
                    assert(bundle == null);
                    assert(drive == null);
                    drive = product;
                    break;
                case BUNDLE:
                    assert(bundle == null);
                    assert(drive == null);
                    drive = product;
                    bundle = product;
                    componentClass = ProductClass.BUNDLE_COMPONENT;
                    break;
                case CUSTOM_BUNDLE:
                    assert(bundle == null);
                    assert(drive == null);
                    drive = product;
                    bundle = product;
                    componentClass = ProductClass.CUSTOM_BUNDLE_COMPONENT;
                    break;
                default:
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

    public BundleBuilder(String mainOfferId, String priceId, String... componentOfferIds) {
        Product product = new ProductBuilder(mainOfferId, "", priceId).build();
        List<Product> products = new ArrayList<>();  // Arrays.asList(product);
        products.add(product);
        for(String componentOfferId: componentOfferIds) {
            Product component = new ProductBuilder(componentOfferId, "", null).build();
            products.add(component);
        }
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
        addComponent(product);
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
            if(drive.getProductOfferingId().equals(product.getProductOfferingId())) {
                drive.setProductId(product.getProductId());
            }
            if(bundle != null && bundle.getProductOfferingId().equals(product.getProductOfferingId())) {
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
        drive.setTarificationPeriod(tarificationPeriod);
        if(bundle != null) { bundle.setTarificationPeriod(tarificationPeriod); }
        for(Product component: components) {
            component.setTarificationPeriod(tarificationPeriod);
        }
        return this;
    }

    public BundleBuilder productStartDate(OffsetDateTime startDate) {
        bundle.setProductStartDate(startDate);
        for(Product component: components) {
            component.setProductStartDate(startDate);
        }
        return this;
    }

    public BundleBuilder status(String status) {
        bundle.setStatus(status);
        for(Product component: components) {
            component.setStatus(status);
        }
        return this;
    }

    public BundleBuilder machineState(JsonNode machineState) {
        drive.setMachineState(machineState);
        return this;
    }

    public TestBundle build() {
        TestBundle testBundle = new TestBundle();
        testBundle.drive = this.drive;
        testBundle.bundle = this.bundle;
        testBundle.components = this.components;
        return testBundle;
    }
}
