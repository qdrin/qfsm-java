package org.qdrin.qfsm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mockserver.client.MockServerClient;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductRelationship;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.repository.*;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Helper {
  // public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
  //     .parse("mockserver/mockserver")
  //     .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

  // @Container
  // public static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);
  
  @Autowired
  ProductRepository productRepository;

  @Autowired
  EventRepository eventRepository;

  @Autowired
  ContextRepository contextRepository;

  @Resource(name = "stateMachinePersist")
  private ProductStateMachinePersist persist;

  @Autowired
  StateMachineService<String, String> service;

  static public ObjectMapper mapper = new ObjectMapper();

  @Value("${application.fsm.time.priceEndedBefore}")
  Duration priceEndedBefore;
  public Duration getPriceEndedBefore() { return priceEndedBefore; }

  @Value("${application.fsm.time.waitingPayInterval}")
  Duration waitingPayInterval;
  public Duration getWaitingPayInterval() { return waitingPayInterval; }

  private static HttpHeaders headers = new HttpHeaders();

  private static TestOffers testOffers = setTestOffers();
  
  public static HttpHeaders getHeaders() {
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

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

  public TestOffers getTestOffers() {
    return testOffers;
  }

  public void clearDb() {
    productRepository.deleteAll();
    contextRepository.deleteAll();
    eventRepository.deleteAll();
  }

  public Product getProduct(String productId) {
    return productRepository.findById(productId).get();
  }

  public static final String[] stateSuit(String... states) {
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


  public static JsonNode buildMachineState(String... states) {
    JsonNode result;
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode provisions = mapper.createArrayNode();
    JsonNode usage = null;
    JsonNode payment = null;
    JsonNode price = null;
    if(states.length == 0) {
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

  public StateMachine<String, String> createMachine(JsonNode machineState, Product product) {
    String machineId = product.getProductId();
    if(product != null) {
      productRepository.save(product);
    }
    if(machineState != null) {
      try {
        // StateMachineContext<String, String> context = createContext(machineState);
        StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
        persist.write(context, machineId);
      } catch(Exception e) {
        log.error("cannot write context to DB: {}", e.getLocalizedMessage());
      }
    }
    StateMachine<String, String> machine = service.acquireStateMachine(machineId);
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    variables.put("actions", new ArrayList<ActionSuit>());
    variables.put("deleteActions", new ArrayList<ActionSuit>());
    variables.put("product", product);
    return machine;
  }

  public StateMachine<String, String> createMachine(JsonNode machineState) {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    return createMachine(machineState, product);
  }

  public StateMachine<String, String> createMachine(Product product) {
    return createMachine(null, product);
  }
  public StateMachine<String, String> createMachine() {
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    return createMachine(product);
  }

  public static class Assertions {

    public static void assertResponseEquals(RequestEventDto request, ResponseEventDto response) {
      assertEquals(request.getEvent().getRefId(), response.getRefId());
      List<ProductResponseDto> rproducts = response.getProducts();
      assertNotNull(rproducts);
      String eventType = request.getEvent().getEventType();
      if(eventType.equals("activation_started")) {
        List<ProductActivateRequestDto> orderItems = request.getProductOrderItems();
        assertEquals(orderItems.size(), rproducts.size());
        for(ProductActivateRequestDto item: orderItems) {
          List<ProductResponseDto> lp = rproducts.stream()
              .filter(p -> p.getProductOrderItemId().equals(item.getProductOrderItemId()))
              .collect(Collectors.toList());
          assertEquals(1, lp.size());
          ProductResponseDto rproduct = lp.get(0);
          assertNotNull(rproduct.getProductId());
          assertEquals(item.getProductOfferingId(), rproduct.getProductOfferingId());
          assertEquals(item.getProductOfferingName(), rproduct.getProductOfferingName());
          assertEquals(item.getIsBundle(), rproduct.getIsBundle());
          assertEquals(item.getIsCustom(), rproduct.getIsCustom());
          assertEquals("PENDING_ACTIVATE", rproduct.getStatus());
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
          assertEquals(1, lp.size());
        }
        for(ProductResponseDto rproduct: rproducts) {
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
      }
    }

    public static void assertProductEquals(Product expected, Product actual) throws Exception {
      if(expected.getProductId() != null) 
        { assertEquals(expected.getProductId(), actual.getProductId()); }
      if(expected.getPartyRoleId() != null) 
        { assertEquals(expected.getPartyRoleId(), actual.getPartyRoleId()); }
      if(expected.getProductOfferingId() != null) 
        { assertEquals(expected.getProductOfferingId(), actual.getProductOfferingId()); }
      if(expected.getProductOfferingName() != null)
        { assertEquals(expected.getProductOfferingName(), actual.getProductOfferingName()); }
      if(expected.getStatus() != null)  
      { assertEquals(expected.getStatus(), actual.getStatus()); }
      if(expected.getProductClass() != -1)
        { assertEquals(expected.getProductClass(), actual.getProductClass()); }
      if(! expected.getProductRelationship().isEmpty())
        { assertEquals(expected.getProductRelationship(), actual.getProductRelationship()); }
      if(expected.getIsBundle() != null)
        { assertEquals(expected.getIsBundle(), actual.getIsBundle()); }
      if(expected.getIsCustom() != null)
        { assertEquals(expected.getIsCustom(), actual.getIsCustom()); }
      if(expected.getTarificationPeriod() != -1)
        { assertEquals(expected.getTarificationPeriod(), actual.getTarificationPeriod()); }
      if(expected.getActiveEndDate() != null)
        { assertEquals(expected.getActiveEndDate(), actual.getActiveEndDate()); }
      if(expected.getTrialEndDate() != null)
        { assertEquals(expected.getTrialEndDate(), actual.getTrialEndDate()); }
      if(expected.getProductStartDate() != null)
        { assertEquals(expected.getProductStartDate(), actual.getProductStartDate()); }
      if(! expected.getProductPrice().isEmpty())
        { assertEquals(expected.getProductPrice(), actual.getProductPrice()); }
      if(! expected.getCharacteristic().isEmpty())
        { assertEquals(expected.getCharacteristic(), actual.getCharacteristic()); }
      if(! expected.getFabricRef().isEmpty())
        { assertEquals(expected.getFabricRef(), actual.getFabricRef()); }
      if(! expected.getLabel().isEmpty())
        { assertEquals(expected.getLabel(), actual.getLabel()); }
      if(! expected.getMetaInfo().isEmpty())
        { assertEquals(expected.getMetaInfo(), actual.getMetaInfo()); }
      if(! expected.getMachineState().isEmpty())
        { JSONAssert.assertEquals(expected.getMachineState().toString(), actual.getMachineState().toString(), false); }
    }

    public static void assertProductEquals(List<Product> expected, List<Product> actual) throws Exception {
      for(Product expectedProduct: expected) {
        Product actualProduct = actual.stream()
              .filter(p -> p.getProductOfferingId().equals(expectedProduct.getProductOfferingId()))
              .findFirst().get();
        assertNotNull(actualProduct);
        assertProductEquals(expectedProduct, actualProduct);
      }
    }
  }

  public List<Product> getResponseProducts(ResponseEventDto response) {
    List<Product> result = new ArrayList<>();
    for(ProductResponseDto rproduct: response.getProducts()) {
      Product product = productRepository.findById(rproduct.getProductId()).get();
      result.add(product);
    }
    return result;
  }
}
