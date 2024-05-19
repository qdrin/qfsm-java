package org.qdrin.qfsm.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.EventBuilder;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import static org.qdrin.qfsm.Helper.buildMachineState;

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
        assertEquals(-1, product.getTarificationPeriod());
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
        assertEquals(ProductClass.BUNDLE.ordinal(), p1.getProductClass());
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
        assertEquals(-1, product.getTarificationPeriod());
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
        List<ProductPrice> empty = new ArrayList();
        assertEquals(empty, product.getProductPrice());
        assertEquals("component1", product.getProductOfferingId());
        assertEquals(ProductClass.VOID.ordinal(), product.getProductClass());
        assertEquals(-1, product.getTarificationPeriod());
        assertEquals("PENDING_ACTIVATE", product.getStatus());
        assertEquals("subscriber1", product.getPartyRoleId());
        assertNotNull(product.getProductId());

        Product p1 = productBuilder
            .productId(UUID.randomUUID().toString())
            .productOfferingId("component2")
            .isCustom(false)
            .build();
        assertEquals("component2", p1.getProductOfferingId());
        assertEquals(ProductClass.VOID.ordinal(), p1.getProductClass());
        assertNotEquals(product.getProductId(), p1.getProductId());
        assertEquals("subscriber1", p1.getPartyRoleId());
        assertEquals(empty, p1.getProductPrice());
    }

    @Test
    public void testBuildMachineStateInitial() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TextNode expected = mapper.getNodeFactory().textNode("Entry");
        JsonNode machineState = buildMachineState();
        System.out.println(String.format("machineState: %s", machineState));
        System.out.println(String.format("expected: %s", expected));
        JSONAssert.assertEquals(expected.toString(), machineState.toString(), false);
    }

    private static Stream<Arguments> testBuildMachineState() {
        return Stream.of(
        Arguments.of(Arrays.asList("PendingActivate"), "'PendingActivate'"),
        Arguments.of(Arrays.asList("Disconnect"), "'Disconnect'"),
        Arguments.of(Arrays.asList("ActiveTrial", "Paid", "PriceActive"),
            "{'Provision': [{'UsageRegion': {'UsageOn': {'Activated': 'ActiveTrial'}}}, {'PaymentRegion': {'PaymentOn': 'Paid'}}, {'PriceRegion': {'PriceOn': 'PriceActive'}}]}"),
        Arguments.of(Arrays.asList("Suspended", "NotPaid", "PriceWaiting"),
            "{'Provision': [{'UsageRegion': {'UsageOn': 'Suspended'}}, {'PaymentRegion': {'PaymentOn': 'NotPaid'}}, {'PriceRegion': {'PriceOn': 'PriceWaiting'}}]}"),
        Arguments.of(Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal"),
            "{'Provision': [{'UsageRegion': 'PendingDisconnect'}, {'PaymentRegion': 'PaymentFinal'}, {'PriceRegion': 'PriceFinal'}]}")
        );
    } 
    @ParameterizedTest
    @MethodSource
    public void testBuildMachineState(List<String> states, String expectedString) throws Exception {

        String exp = expectedString.replace("'", "\"");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expected = mapper.readTree(exp);
        JsonNode machineState = buildMachineState(states);
        JSONAssert.assertEquals(expected.toString(), machineState.toString(), false);
    }

    @Test
    public void EventBuilderActivateSimple() throws Exception {
        EventBuilder eventBuilder = new EventBuilder("activation_started", "simpleOffer1", "simple1-price-trial");
        RequestEventDto event = eventBuilder.build();
        log.debug("event: {}", event);
        assertEquals("activation_started", event.getEvent().getEventType());
        assert(event.getEvent().getEventDate().isBefore(OffsetDateTime.now().plusSeconds(1)));
        assert(event.getEvent().getEventDate().isAfter(OffsetDateTime.now().minusSeconds(1)));
        assertEquals(1, event.getProductOrderItems().size());
        assertNull(event.getProducts());
        assertEquals("simpleOffer1", event.getProductOrderItems().get(0).getProductOfferingId());
    }

    @Test
    public void EventBuilderAbortSimple() throws Exception {
        Product product = new ProductBuilder("simpleOffer1", "PENDING_ACTIVATE", "simple1-price-trial").build();
        EventBuilder eventBuilder = new EventBuilder("activation_aborted", product);
        RequestEventDto event = eventBuilder.build();
        log.debug("event: {}", event);
        assertEquals("activation_aborted", event.getEvent().getEventType());
        assert(event.getEvent().getEventDate().isBefore(OffsetDateTime.now().plusSeconds(1)));
        assert(event.getEvent().getEventDate().isAfter(OffsetDateTime.now().minusSeconds(1)));
        assertEquals(1, event.getProducts().size());
        assertNull(event.getProductOrderItems());
        assertEquals(product.getProductId(), event.getProducts().get(0).getProductId());
    }
}
