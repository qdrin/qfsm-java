package org.qdrin.qfsm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qdrin.qfsm.TestOffers.OfferDef;
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.ClientInfo;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.dto.EventDto;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.repository.*;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.stereotype.Component;

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
@Component
public class Helper {
  
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

  final TestOffers testOffers = setTestOffers();

  private TestOffers setTestOffers() {
    ClassPathResource resource = new ClassPathResource("/offers.yaml", getClass());
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                              // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    TestOffers offers = null;
    try {
      offers = yamlMapper.readValue(resource.getInputStream(), TestOffers.class);
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

  public static class TestEvent {
    public final RequestEventDto requestEvent;

    public static class Builder {
      public String eventType = "activation_started";
      public String sourceCode = "OMS";
      public String refId = UUID.randomUUID().toString();
      public String refIdType = "orderId";
      public String partyRoleId = UUID.randomUUID().toString();
      public String partyId = UUID.randomUUID().toString();
      public List<ProductRequestDto> products = null;
      public List<ProductActivateRequestDto> productOrderItems = null;
      public List<Characteristic> characteristics = null;
      public EventProperties eventProperties = null;

      public Builder() {}

      public Builder eventType(String val) { 
        eventType = val;
        return this;
      }

      public Builder sourceCode(String val) { 
        sourceCode = val;
        return this;
      }
      public Builder refId(String val) {
        refId = val;
        return this;
      }
      public Builder refIdType(String val) {
        refIdType = val;
        return this;
      }
      public Builder partyRoleId(String val) {
        partyRoleId = val;
        return this;
      }
      public Builder partyId(String val) {
        partyId = val;
        return this;
      }
      public Builder product(ProductRequestDto product) {
        if(products == null) {
          products = new ArrayList<>();
        }
        products.add(product);
        return this;
      }
      public Builder products(List<ProductRequestDto> products) {
        this.products = products;
        return this;
      }
      public Builder productOrderItem(ProductActivateRequestDto item) {
        if(productOrderItems == null) {
          productOrderItems = new ArrayList<>();
        }
        productOrderItems.add(item);
        return this;
      }
      public Builder productOrderItems(List<ProductActivateRequestDto> items) {
        this.productOrderItems = items;
        return this;
      }
      public Builder characteristics(List<Characteristic> chars) {
        this.characteristics = chars;
        return this;
      }
      public Builder eventProperties(EventProperties props) {
        eventProperties = props;
        return this;
      }

      public TestEvent build() {
        return new TestEvent(this);
      }
    }

    private TestEvent(Builder builder) {
      requestEvent = new RequestEventDto();
      EventDto event = new EventDto();
      event.setEventType(builder.eventType);
      event.setRefId(builder.refId);
      event.setRefIdType(builder.refIdType);
      event.setSourceCode(builder.sourceCode);
      requestEvent.setEvent(event);
      requestEvent.setCharacteristics(builder.characteristics);
      requestEvent.setEventProperties(builder.eventProperties);
      switch(builder.eventType) {
        case "activation_started":
            ClientInfo clientInfo = new ClientInfo();
            clientInfo.setPartyId(builder.partyId);
            clientInfo.setPartyRoleId(builder.partyRoleId);
            requestEvent.setClientInfo(clientInfo);
            requestEvent.setProductOrderItems(builder.productOrderItems);
            break;
        default:
            requestEvent.setProducts(builder.products);
      }
    }
  }

  public List<ProductActivateRequestDto> buildOrderItems(
        String mainOfferId,
        String priceId,
        String... componentOffers) {
    Map<String, OfferDef> offers = getTestOffers().getOffers();
    log.debug("componentOffers: {}", componentOffers);
    ArrayList<ProductActivateRequestDto> items = new ArrayList<>();
    ProductActivateRequestDto bundle = new ProductActivateRequestDto();
    String bundleItemId = UUID.randomUUID().toString();
    List<ProductOrderItemRelationshipDto> relations = new ArrayList<>();
    OfferDef bundleOffer = offers.get(mainOfferId);
    ProductPrice price = bundleOffer.getPrices().get(priceId);
    price.setId(priceId);
    String relationType = bundleOffer.getIsCustom() ? "CUSTOM_BUNDLES" : "BUNDLES";
    bundle.setProductOrderItemId(bundleItemId);
    bundle.setProductOfferingId(mainOfferId);
    bundle.setProductOfferingName(bundleOffer.getName());
    bundle.setFabricRef(bundleOffer.getFabricRef());
    bundle.setIsBundle(bundleOffer.getIsBundle());
    bundle.setIsCustom(bundleOffer.getIsCustom());
    bundle.setProductPrice(Arrays.asList(price));
    items.add(bundle);
    if(componentOffers != null) {
      bundle.setProductOrderItemRelationship(relations);
      for(int i = 0; i < componentOffers.length; i++) {
        OfferDef componentOffer = offers.get(componentOffers[i]);
        ProductActivateRequestDto component = new ProductActivateRequestDto();
        String componentItemId = UUID.randomUUID().toString();
        component.setProductOrderItemId(componentItemId);
        component.setProductOfferingName(componentOffer.getName());
        component.setIsBundle(false);
        component.setIsCustom(false);
        component.setProductOfferingId(componentOffers[i]);
        ProductOrderItemRelationshipDto rel = new ProductOrderItemRelationshipDto();
        rel.setProductOrderItemId(componentItemId);
        rel.setRelationshipType(relationType);  // TODO: CUSTOM_BUNDLES???
        relations.add(rel);
        items.add(component);
      }
    }
    return items;
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
}
