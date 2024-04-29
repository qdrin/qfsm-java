package org.qdrin.qfsm.unit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.Helper;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelperTest {
    @Test
    public void ProductBuilderDefaults() throws Exception {
        ProductBuilder productBuilder = new ProductBuilder("simpleOffer1", "ACTIVE", "simple1-price-active");
        Product product = productBuilder.build();
        log.debug("product: {}", product);
        assertEquals("simple1-price-active", product.getProductPrice().get(0).getId());
        assertEquals("simpleOffer1", product.getProductOfferingId());
        assertEquals(1, product.getProductClass());
        assertEquals(0, product.getTarificationPeriod());
        assertEquals("ACTIVE", product.getStatus());
        assertNotNull(product.getPartyRoleId());
        assertNotNull(product.getProductId());

        Product p1 = productBuilder
                .productOfferingId("bundleOffer1")
                .priceId("bundle1-price-trial")
                .tarificationPeriod(3)
                .status("PENDING_DISCONNECT")
                .build();
        assertEquals("bundleOffer1", p1.getProductOfferingId());
        assertEquals("bundle1-price-trial", p1.getProductPrice().get(0).getId());
        assertEquals(2, p1.getProductClass());
        assertEquals(3, p1.getTarificationPeriod());
        assertEquals("PENDING_DISCONNECT", p1.getStatus());
        assertEquals(p1.getPartyRoleId(), product.getPartyRoleId());
        assertNotNull(p1.getProductId(), product.getProductId());
    }

    @Test
    public void ProductBuilderCustomBundle() throws Exception {
        ProductBuilder productBuilder = new ProductBuilder("customBundleOffer1", null, "custom1-price-active");
        Product product = productBuilder.build();
        log.debug("product: {}", product);
        assertEquals("custom1-price-active", product.getProductPrice().get(0).getId());
        assertEquals("customBundleOffer1", product.getProductOfferingId());
        assertEquals(4, product.getProductClass());
        assertEquals(0, product.getTarificationPeriod());
        assertEquals("PENDING_ACTIVATE", product.getStatus());
        assertNotNull(product.getPartyRoleId());
        assertNotNull(product.getProductId());
    }

    @Test
    public void ProductBuilderComponent() throws Exception {
        ProductBuilder productBuilder = new ProductBuilder("component1", null, null);
        Product product = productBuilder
            .isCustom(true)
            .partyRoleId("subscriber1")
            .build();
        log.debug("product: {}", product);
        assertNull(product.getProductPrice());
        assertEquals("component1", product.getProductOfferingId());
        assertEquals(5, product.getProductClass());
        assertEquals(0, product.getTarificationPeriod());
        assertEquals("PENDING_ACTIVATE", product.getStatus());
        assertEquals("subscriber1", product.getPartyRoleId());
        assertNotNull(product.getProductId());

        Product p1 = productBuilder
            .productOfferingId("component2")
            .isCustom(false)
            .build();
        assertEquals("component2", p1.getProductOfferingId());
        assertEquals(3, p1.getProductClass());
        assertNotEquals(product.getProductId(), p1.getProductId());
        assertEquals("subscriber1", p1.getPartyRoleId());
        assertNull(p1.getProductPrice());
    }

    @Test
    public void testBuildMachineState() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode expected = mapper.createObjectNode();
        ArrayNode provisions = mapper.createArrayNode();
        provisions.add(
                mapper.createObjectNode().set("UsageOn", mapper.createObjectNode().put("Activated", "ActiveTrial")))
                .add(mapper.createObjectNode().put("PaymentOn", "Paid"))
                .add(mapper.createObjectNode().put("PriceOn", "PriceActive"));
        JsonNode machineState = Helper.buildMachineState("ActiveTrial", "Paid", "PriceActive");
        expected.set("Provision", provisions);
        log.debug("machineState: {}", machineState);
        log.debug("expected: {}", expected);
        JSONAssert.assertEquals(expected.toString(), machineState.toString(), false);
    }
}
