package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.repository.ProductRepository;
import org.qdrin.qfsm.service.QStateMachineContextConverter;

import com.fasterxml.jackson.databind.JsonNode;

// @FieldDefaults(level = AccessLevel.PUBLIC)
public class BundleBuilder {

    public class TestBundle {
        public Product drive;
        public Product bundle;
        public List<Product> products;
        public List<Product> components() {
            List<Product> res = new ArrayList<>();
            for(Product product: products) {
                if(product == drive || product == bundle) {
                    continue;
                } else {
                    res.add(product);
                }
            }
            return res;
        }

        public Product getByOfferId(String offerId) {
            return products.stream().filter(p -> p.getProductOfferingId().equals(offerId)).findFirst().get();
        }
    }

    String partyRoleId = UUID.randomUUID().toString();
    String eventType = null;
    ProductClass componentClass = null;
    ProductClass bundleClass = null;

    List<Product> products = new ArrayList<>();  // ful product set of bundle
    Product drive = null;   // Working product. Can be bundle or custom_bundle component or simple
    Product bundle = null;
    List<Product> components = new ArrayList<>();  // sublist of Products

    private void setComponentClass(ProductClass cclass) {
        for(Product component: components) {
            component.setProductClass(cclass.ordinal());     
        }
    }

    private void createFromProducts() {
        List<ProductRelationship> relations = new ArrayList<>();
        for(Product product: products) {
            ProductClass pclass = ProductClass.values()[product.getProductClass()];
            switch(pclass) {
                case SIMPLE:
                    assert(products.size() == 1);
                    bundle = null;
                    drive = product;
                    break;
                case BUNDLE:
                    assert(bundle == null);
                    drive = product;
                    bundle = product;
                    componentClass = ProductClass.BUNDLE_COMPONENT;
                    product.setProductRelationship(relations);
                    break;
                case CUSTOM_BUNDLE:
                    assert(bundle == null);
                    drive = product;
                    bundle = product;
                    product.setProductRelationship(relations);
                    componentClass = ProductClass.CUSTOM_BUNDLE_COMPONENT;
                    break;
                case CUSTOM_BUNDLE_COMPONENT:
                    if(drive == product) {
                        break;
                    }
                default:
                    ProductRelationship rel = new ProductRelationship(product);
                    relations.add(rel);
                    components.add(product);
            }
        }
        if(componentClass != null) {
            setComponentClass(componentClass);
            String relType = componentClass == ProductClass.BUNDLE_COMPONENT ? "BUNDLES" : "CUSTOM_BUNDLES";
            bundle.getProductRelationship().forEach((r) -> {r.setRelationshipType(relType);});
        }
    }

    public BundleBuilder(TestBundle testBundle) {
        boolean isBundleInProducts = false;
        for(Product product: testBundle.products) {
            Product copy = new ProductBuilder(product).build();
            products.add(copy);
            if(product == testBundle.drive) {
                drive = copy;
            }
            if(product == testBundle.bundle) {
                isBundleInProducts = true;
            }
        }
        if(! isBundleInProducts && testBundle.bundle != null) {
            this.bundle = new ProductBuilder(testBundle.bundle).build();
        }
        components = new ArrayList<>();
        for(Product product: products) {
            if(product == drive) continue;
            components.add(product);
        }
    }

    public BundleBuilder(String mainOfferId, String priceId, List<String> componentOfferIds) {
        this(mainOfferId, priceId, componentOfferIds.toArray(new String[0]));
    }

    public BundleBuilder(String mainOfferId, String priceId, String... componentOfferIds) {
        Product product = new ProductBuilder(mainOfferId, "", priceId).build();
        drive = product;
        drive.getMachineContext().setIsIndependent(true);
        bundleClass = ProductClass.values()[product.getProductClass()];
        products.add(product);
        if(componentOfferIds == null) {
            componentOfferIds = new String[] {};
        }
        for(String componentOfferId: componentOfferIds) {
            Product component = new ProductBuilder(componentOfferId, "", null).build();
            switch(bundleClass) {
                case BUNDLE:
                    componentClass = ProductClass.BUNDLE_COMPONENT;
                    break;
                case CUSTOM_BUNDLE:
                    componentClass = ProductClass.CUSTOM_BUNDLE_COMPONENT;
                    break;
                default:
                    componentClass = ProductClass.VOID;                 
            }
            products.add(component);
        }
        createFromProducts();
    }

    public BundleBuilder(String offerId, String priceId) {
        this(offerId, priceId, new ArrayList<>());
    }

    public BundleBuilder(RequestEventDto event) {
        String eventType = event.getEvent().getEventType();
        this.eventType = eventType;
        if(eventType.equals("activation_started")) {
            this.partyRoleId = event.getClientInfo().getPartyRoleId();
            for(ProductActivateRequestDto item: event.getProductOrderItems()) {
                Product product = new ProductBuilder(item)
                        .productId(null)
                        .partyRoleId(partyRoleId)
                        .build();
                products.add(product);
            }
        }
        // } else {  // Обработка все событий, кроме activation_started
        //     for(ProductRequestDto item: event.getProducts()) {
        //         Product product = productRepository.findById(item.getProductId()).get();
        //         assert(product != null);
        //         products.add(product);
        //     }        
        // }
        createFromProducts();
    }

    public BundleBuilder componentClass(ProductClass componentClass) {
        this.componentClass = componentClass;
        setComponentClass(componentClass);
        return this;
    }

    public BundleBuilder driveClass(ProductClass driveClass) {
        drive.setProductClass(driveClass.ordinal());
        return this;
    }

    public BundleBuilder addBundle(Product bundle) {
        assert(this.bundle == null);
        this.bundle = bundle;
        return this;
    }

    public BundleBuilder addComponent(Product component) {
        components.add(component);
        ProductRelationship relation = new ProductRelationship(component);
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

    public BundleBuilder productIds(List<Product> idsProducts) {
        assert(idsProducts.size() == products.size());
        for(Product idProduct: idsProducts) {
            for(Product product: products) {
                if(product.getProductOfferingId().equals(idProduct.getProductOfferingId())) {
                    product.setProductId(idProduct.getProductId());
                    continue;
                }
            }
        }
        bundle = null;
        createFromProducts();
        return this;
    }

    public BundleBuilder tarificationPeriod(int tarificationPeriod) {
        List<ProductClass> isTarificated = Arrays.asList(ProductClass.SIMPLE, ProductClass.BUNDLE, ProductClass.CUSTOM_BUNDLE);
        if(isTarificated.contains(ProductClass.values()[drive.getProductClass()])) {
            drive.setTarificationPeriod(tarificationPeriod);
        }
        return this;
    }

    public BundleBuilder productStartDate(OffsetDateTime startDate) {
        for(Product product: products) {
            product.setProductStartDate(startDate);
        }
        return this;
    }

    public BundleBuilder status(String status) {
        products.stream().forEach(p -> p.setStatus(status));
        return this;
    }

    public BundleBuilder trialEndDate(OffsetDateTime date) {
        for(Product product: products) {
            product.setTrialEndDate(date);
        }
        return this;
    }

    public BundleBuilder activeEndDate(OffsetDateTime date) {
        for(Product product: products) {
            product.setActiveEndDate(date);
        }
        return this;
    }

    public BundleBuilder pricePeriod(int period) {
        Optional<ProductPrice> price = drive.getProductPrice(PriceType.RecurringCharge);
        assert(price.isPresent());
        price.get().setPeriod(period);
        return this;
    }

    public BundleBuilder priceNextPayDate(OffsetDateTime date) {
        Optional<ProductPrice> price = drive.getProductPrice(PriceType.RecurringCharge);
        assert(price.isPresent());
        price.get().setNextPayDate(date);
        return this;
    }

    public BundleBuilder machineState(JsonNode machineState) {
        drive.getMachineContext().setMachineState(machineState);
        JsonNode componentMachineState = machineState != null ? QStateMachineContextConverter.buildComponentMachineState(machineState) : null;
        components.stream()
            .filter(c -> ! c.getMachineContext().getIsIndependent())
            .forEach(c -> c.getMachineContext().setMachineState(componentMachineState));
        return this;
    }

    public BundleBuilder componentMachineState(JsonNode componentState) {
        components.stream()
            .filter(c -> ! c.getMachineContext().getIsIndependent())
            .forEach(c -> c.getMachineContext().setMachineState(componentState));
        return this;
    }

    public BundleBuilder isIndependent(boolean val) {
        drive.getMachineContext().setIsIndependent(val);
        return this;
    }

    public BundleBuilder save(ProductRepository productRepository) {
        for(Product product: products) {
            productRepository.save(product);
        }
        return this;
    }

    public TestBundle build() {
        TestBundle testBundle = new TestBundle();
        testBundle.drive = this.drive;
        testBundle.bundle = this.bundle;
        testBundle.products = this.products;
        return testBundle;
    }
}
