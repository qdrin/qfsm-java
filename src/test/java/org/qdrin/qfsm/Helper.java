package org.qdrin.qfsm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.qdrin.qfsm.TestSetup.TestSetupBuilder;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.ProductRelationship;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Helper {


  static public ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.registerModule(new JavaTimeModule());
  }

  public static TestOffers testOffers = setTestOffers();

  public static String readResourceAsString(ClassPathResource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), "UTF8")) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static TestOffers setTestOffers() {
    ClassPathResource resource = new ClassPathResource("/offers.yaml", Yaml.class);
    String offerString = readResourceAsString(resource);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                              // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    TestOffers offers = null;
    try {
      offers = yamlMapper.readValue(offerString, TestOffers.class);
    } catch (StreamReadException e) {
      e.printStackTrace();
    } catch (DatabindException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return offers;
  }

  private static final List<TestSetup> testSetups = buildTestSetups();
  private static List<TestSetup> buildTestSetups() {
    List<TestSetup> result = new ArrayList<>();
    List<String> components = Arrays.asList("component1", "component2", "component3");
    TestSetupBuilder builder = new TestSetupBuilder();
    
    result.add(builder.offerId("simpleOffer1").priceId("simple1-price-trial")
      .productClass(ProductClass.SIMPLE)
      .build());
    result.add(builder.priceId("simple1-price-active").build());
    
    result.add(builder.offerId("bundleOffer1").priceId("bundle1-price-trial")
      .productClass(ProductClass.BUNDLE)
      .componentOfferIds(new ArrayList<>(components))
      .build());
    result.add(builder.priceId("bundle1-price-active").build());

    result.add(builder.offerId("customBundleOffer1").priceId("custom1-price-trial")
    .productClass(ProductClass.CUSTOM_BUNDLE)
    .componentOfferIds(new ArrayList<>(components))
    .build());
  result.add(builder.priceId("custom1-price-active").build());
  return result;
  }

  public static List<TestSetup> getTestSetups() {
    List<TestSetup> copy = new ArrayList<>();
    for(TestSetup arg: testSetups) {
      copy.add(arg.toBuilder().build());
    }
    return copy;
  }

  public static final String[] stateSuite(List<String> states) {
    return stateSuite(states.toArray(new String[0]));
  }

  public static final String[] stateSuite(String... states) {
    List<String> stateList = new ArrayList<>();
    boolean isProvision = false;
    for(String s: states) {
      switch(s) {
        case "PendingDisconnect":
        case "Disconnection":
        case "UsageFinal":
        case "PaymentStopping":
        case "PaymentStopped":
        case "PaymentFinal":
        case "PriceOff":
        case "PriceFinal":
          isProvision = true;
          stateList.add(s);
          break;
        case "Prolongation":
        case "Suspending":
        case "Resuming":
        case "Suspended":
          isProvision = true;
          stateList.addAll(Arrays.asList("UsageOn", s));
          break;
        case "Active":
        case "ActiveTrial":
          isProvision = true;
          stateList.addAll(Arrays.asList("UsageOn", "Activated", s));
          break;
        case "Paid":
        case "WaitingPayment":
        case "NotPaid":
          isProvision = true;
          stateList.addAll(Arrays.asList("PaymentOn", s));
          break;
        case "PriceActive":
        case "PriceChanging":
        case "PriceChanged":
        case "PriceNotChanged":
        case "PriceWaiting":
          isProvision = true;
          stateList.addAll(Arrays.asList("PriceOn", s));
          break;
        default:
          stateList.add(s);
      }
    }
    if(isProvision) {
      stateList.add("Provision");
    }
    String[] res = stateList.toArray(new String[0]);
    return res;
  }

  public static JsonNode buildMachineState(List<String> states) {
    return buildMachineState(states.toArray(new String[0]));
  }

  public static JsonNode buildMachineState(String... states) {
    JsonNode result;
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode provisions = mapper.createArrayNode();
    JsonNode usage = null;
    JsonNode payment = null;
    JsonNode price = null;
    if(states == null || states.length == 0) {
      result = mapper.getNodeFactory().textNode("Entry");
    } else if(states.length == 1) {
      result = mapper.getNodeFactory().textNode(states[0]);
    } else {
      ObjectNode res = mapper.createObjectNode();
      res.set("Provision", provisions);
      result = res;
      for(String s: states) {
        switch(s) {
          case "PendingDisconnect":
          case "Disconnection":
          case "UsageFinal":
            usage = mapper.getNodeFactory().textNode(s);
            break;
          case "PaymentStopping":
          case "PaymentStopped":
          case "PaymentFinal":
            payment = mapper.getNodeFactory().textNode(s);
            break;
          case "PriceOff":
          case "PriceFinal":
            price = mapper.getNodeFactory().textNode(s);
            break;
          case "Prolongation":
          case "Suspending":
          case "Resuming":
          case "Suspended":
            usage = mapper.createObjectNode().put("UsageOn", s);
            break;
          case "Active":
          case "ActiveTrial":
            usage = mapper.createObjectNode().set("UsageOn", mapper.createObjectNode().put("Activated", s));
            break;
          case "Paid":
          case "WaitingPayment":
          case "NotPaid":
            payment = mapper.createObjectNode().put("PaymentOn", s);
            break;
          case "PriceActive":
          case "PriceChanging":
          case "PriceChanged":
          case "PriceNotChanged":
          case "PriceWaiting":
            price = mapper.createObjectNode().put("PriceOn", s);
            break;
          default:
            result = mapper.getNodeFactory().textNode(states[0]);
        }
      }
      provisions.add(usage).add(payment).add(price);
    }  
    return result;
  }

  public static JsonNode buildComponentMachineState(JsonNode bundleMachineState) {
    if(bundleMachineState == null) { return null; }
    if(bundleMachineState.has("Provision")) {
      ObjectNode res = mapper.createObjectNode();
      JsonNode usage = bundleMachineState.get("Provision").get(0);
      ArrayNode provision = res.putArray("Provision");
      provision.add(usage);
      provision.add("PaymentFinal");
      provision.add("PriceFinal");
      return res;
    } else {
      return bundleMachineState.deepCopy();
    }
  }

  public static ProductClass getComponentClass(ProductClass bundleClass) {
    switch(bundleClass) {
      case BUNDLE: return ProductClass.BUNDLE_COMPONENT;
      case CUSTOM_BUNDLE: return ProductClass.CUSTOM_BUNDLE_COMPONENT;
      default: return ProductClass.VOID;
    }
  }

  public static Stream<Arguments> provideTestSetups() {
    List<TestSetup> copy = getTestSetups();
    Stream<Arguments> stream = copy.stream().map(arg->Arguments.of(arg)).distinct();
    return stream;
  }

  public static class Assertions {

    public static void assertResponseEquals(RequestEventDto request, ResponseEventDto response) {
      assertEquals(request.getEvent().getRefId(), response.getRefId(), "refId");
      List<ProductResponseDto> rproducts = response.getProducts();
      assertNotNull(rproducts, "response products is null");
      String eventType = request.getEvent().getEventType();
      if(eventType.equals("activation_started")) {
        List<ProductActivateRequestDto> orderItems = request.getProductOrderItems();
        assertEquals(orderItems.size(), rproducts.size());
        for(ProductActivateRequestDto item: orderItems) {
          List<ProductResponseDto> lp = rproducts.stream()
              .filter(p -> p.getProductOrderItemId().equals(item.getProductOrderItemId()))
              .collect(Collectors.toList());
          assertEquals(1, lp.size(), String.format("productOfferingId: %s", item.getProductOfferingId()));
          ProductResponseDto rproduct = lp.get(0);
          assertNotNull(rproduct.getProductId(), "productId is null");
          assertEquals(item.getProductOfferingId(), rproduct.getProductOfferingId(), "productOfferingId");
          assertEquals(item.getProductOfferingName(), rproduct.getProductOfferingName(), "productOfferingName");
          assertEquals(item.getIsBundle(), rproduct.getIsBundle(), "isBundle");
          assertEquals(item.getIsCustom(), rproduct.getIsCustom(), "isCustom");
          assertEquals("PENDING_ACTIVATE", rproduct.getStatus(), "status");
          if(rproduct.getIsBundle()) {
            List<ProductRelationship> relations = rproduct.getProductRelationship();
            if(relations == null) {
              relations = new ArrayList<>();
            }
            assertEquals(rproducts.size()-1, relations.size());
            for(ProductRelationship r: relations) {
              List<ProductResponseDto> lrp = rproducts.stream()
                .filter(p -> p.getProductId().equals(r.getProductId()))
                .collect(Collectors.toList());
              assertEquals(1, lrp.size());
            }
          }
        }
      } else {
        List<ProductRequestDto> orderItems = request.getProducts();
        assert(orderItems.size() <= rproducts.size());
        for(ProductRequestDto item: orderItems) {
          List<ProductResponseDto> lp = rproducts.stream()
              .filter(p -> p.getProductId().equals(item.getProductId()))
              .collect(Collectors.toList());
          assertEquals(1, lp.size(),  String.format("productId: %s", item.getProductId()));
        }
        for(ProductResponseDto rproduct: rproducts) {
          if(rproduct.getIsBundle()) {
            List<ProductRelationship> relations = rproduct.getProductRelationship();
            if(relations == null) {
              relations = new ArrayList<>();
            }
            assertEquals(rproducts.size()-1, relations.size(), "relationship size mismatch");
            for(ProductRelationship r: relations) {
              List<ProductResponseDto> lrp = rproducts.stream()
              .filter(p -> p.getProductId().equals(r.getProductId()))
              .collect(Collectors.toList());
            assertEquals(1, lrp.size(), String.format("relation not found: %s", r.getProductId()));
            }
          }
        }
      }
    }

    public static void assertDates(OffsetDateTime expected, OffsetDateTime real, String message, int delta) {
      assert expected != null : "expeted " + message + " is null";
      assert real != null : "real " + message + " is null";
      OffsetDateTime t0 = expected.minusSeconds(delta);
      OffsetDateTime t1 = expected.plusSeconds(delta);
      assert(real.isBefore(t1)) : String.format("%s : %s is not before %s", message, real, t1);
      assert(real.isAfter(t0)) : String.format("%s : %s is not after %s", message, real, t0);
    }

    public static void assertDates(OffsetDateTime expected, OffsetDateTime real) {
      assertDates(expected, real, null, 10);
    }

    public static void assertDates(OffsetDateTime expected, OffsetDateTime real, String message) {
      assertDates(expected, real, message, 10);
    }

    public static void assertPriceEquals(ProductPrice expected, ProductPrice actual) {
      if(expected.getId() != null)
        { assertEquals(expected.getId(), actual.getId(), "id"); }
      if(expected.getName() != null)
        { assertEquals(expected.getName(), actual.getName(), "name"); }
      if(expected.getPriceType() != null)
        { assertEquals(expected.getPriceType(), actual.getPriceType(), "priceType"); }
      if(expected.getProductStatus() != null)
        { assertEquals(expected.getProductStatus(), actual.getProductStatus(), "productStatus"); }
      if(expected.getRecurringChargePeriodType() != null)
        { assertEquals(expected.getRecurringChargePeriodType(), actual.getRecurringChargePeriodType(), "recurringChargePeriodType"); }
      if(expected.getRecurringChargePeriodLength() != -1)
        { assertEquals(expected.getRecurringChargePeriodLength(), actual.getRecurringChargePeriodLength(), "recurringChargePeriodLength"); }
      if(expected.getDuration() != -1)
        { assertEquals(expected.getDuration(), actual.getDuration(), "duration"); }
      if(expected.getPeriod() != -1)
        { assertEquals(expected.getPeriod(), actual.getPeriod(), "period"); }
      if(expected.getNextPayDate() != null)
        { assertDates(expected.getNextPayDate(), actual.getNextPayDate(), "nextPayDate"); }
      if(expected.getTarificationTag() != null)
        { assertEquals(expected.getTarificationTag(), actual.getTarificationTag(), "tarificationTag"); }
      if(expected.getNextEntity() != null)
        { assertEquals(expected.getNextEntity(), actual.getNextEntity(), "nextEntity"); }
      if(expected.getPriceAlterations() != null)
        { assertEquals(expected.getPriceAlterations(), actual.getPriceAlterations(), "priceAlterations"); }
      if(expected.getTax() != null)
        { assertEquals(expected.getTax(), actual.getTax(), "tax"); }
      if(expected.getPrice() != null)
        { assertEquals(expected.getPrice(), actual.getPrice(), "price"); }
      if(expected.getUnitOfMeasure() != null)
        { assertEquals(expected.getUnitOfMeasure(), actual.getUnitOfMeasure(), "unitOfMeasure"); }
      if(expected.getValidFor() != null)
        { assertEquals(expected.getValidFor(), actual.getValidFor(), "validFor"); }
      if(expected.getHref() != null)
        { assertEquals(expected.getHref(), actual.getHref(), "href"); }
      if(expected.getPsiSpecific() != null)
        { assertEquals(expected.getPsiSpecific(), actual.getPsiSpecific(), "psiSpecific"); }
      if(expected.getValue() != null)
        { assertEquals(expected.getValue(), actual.getValue(), "value"); }
    }

    public static void assertPriceEquals(List<ProductPrice> expected, List<ProductPrice> actual) {
      for(ProductPrice expectedPrice: expected) {
        Optional<ProductPrice> oactualPrice = actual.stream()
              .filter(p -> p.getId().equals(expectedPrice.getId()))
              .findFirst();
        assert(oactualPrice.isPresent()) : String.format("price not found: %s", expectedPrice.getId());
        assertPriceEquals(expectedPrice, oactualPrice.get());
      }
    }

    public static void assertProductEquals(Product expected, Product actual) throws Exception {
      if(expected.getProductId() != null) 
        { assertEquals(expected.getProductId(), actual.getProductId(), "productId"); }
      if(expected.getPartyRoleId() != null) 
        { assertEquals(expected.getPartyRoleId(), actual.getPartyRoleId(), "partyRoleId"); }
      if(expected.getProductOfferingId() != null) 
        { assertEquals(expected.getProductOfferingId(), actual.getProductOfferingId(), "productOfferingId"); }
      if(expected.getProductOfferingName() != null)
        { assertEquals(expected.getProductOfferingName(), actual.getProductOfferingName(), "productOfferingName"); }
      if(expected.getStatus() != null)  
      { assertEquals(expected.getStatus(), actual.getStatus(), String.format("offerId: %s, status", expected.getProductOfferingId())); }
      if(expected.getProductClass() != -1)
        { assertEquals(expected.getProductClass(), actual.getProductClass(), "productClass"); }
      if(! expected.getProductRelationship().isEmpty())
        { assertEquals(expected.getProductRelationship(), actual.getProductRelationship(), "productRelationship"); }
      if(expected.getIsBundle() != null)
        { assertEquals(expected.getIsBundle(), actual.getIsBundle(), "isBundle"); }
      if(expected.getIsCustom() != null)
        { assertEquals(expected.getIsCustom(), actual.getIsCustom(), "isCustom"); }
      if(expected.getTarificationPeriod() != -1)
        { assertEquals(expected.getTarificationPeriod(), actual.getTarificationPeriod(), "tarificationPeriod"); }
      if(expected.getActiveEndDate() != null)
        { assertDates(expected.getActiveEndDate(), actual.getActiveEndDate(), "activeEndDate"); }
      if(expected.getTrialEndDate() != null)
        { assertDates(expected.getTrialEndDate(), actual.getTrialEndDate(), "trialEndDate"); }
      if(expected.getProductStartDate() != null)
        { assertDates(expected.getProductStartDate(), actual.getProductStartDate(), "productStartDate"); }
      if(! expected.getProductPrice().isEmpty())
        { assertPriceEquals(expected.getProductPrice(), actual.getProductPrice()); }
      if(! expected.getCharacteristic().isEmpty())
        { assertEquals(expected.getCharacteristic(), actual.getCharacteristic(), "characteristic"); }
      if(! expected.getFabricRef().isEmpty())
        { assertEquals(expected.getFabricRef(), actual.getFabricRef(), "fabricRef"); }
      if(! expected.getLabel().isEmpty())
        { assertEquals(expected.getLabel(), actual.getLabel(), "label"); }
      if(! expected.getMetaInfo().isEmpty())
        { assertEquals(expected.getMetaInfo(), actual.getMetaInfo(), "metaInfo"); }
      if(expected.getMachineContext().getIsIndependent() != null)
        { assertEquals(expected.getMachineContext().getIsIndependent(), actual.getMachineContext().getIsIndependent()); }
      if(expected.getMachineContext().getMachineState() != null){
        String exp = expected.getMachineContext().getMachineState().toString();
        String act = actual.getMachineContext().getMachineState().toString();
        try {
          JSONAssert.assertEquals(exp, act, false);
        } catch (AssertionError ae) {
          assert false : String.format("machineState. expected: %s, actual: %s", exp, act);
          }
      }
    }

    public static void assertProductEquals(List<Product> expected, List<Product> actual) throws Exception {
      for(Product expectedProduct: expected) {
        Optional<Product> oactualProduct = actual.stream()
              .filter(p -> p.getProductOfferingId().equals(expectedProduct.getProductOfferingId()))
              .findFirst();
        assert(oactualProduct.isPresent()) : String.format("product not found: %s", expectedProduct.getProductId());
        assertProductEquals(expectedProduct, oactualProduct.get());
      }
    }
  }
}
