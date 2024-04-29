package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.access.StateMachineAccess;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class StateMachineTest {

  @Resource(name = "stateMachinePersist")
  private ProductStateMachinePersist persist;

  @Autowired
  StateMachineService<String, String> service;

  @Autowired
  Helper helper;

  private StateMachine<String, String> machine;

  private final String[] stateSuit(String... states) {
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

  @BeforeEach
  public void setup() throws Exception {
    helper.clearDb();
  }

  ObjectMapper mapper = new ObjectMapper();

  public StateMachine<String, String> createMachine(JsonNode machineState, String machineId) {
    if(machineState != null) {
      try {
        // StateMachineContext<String, String> context = createContext(machineState);
        StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
        persist.write(context, machineId);
      } catch(Exception e) {
        log.error("cannot write context to DB: {}", e.getLocalizedMessage());
      }
    }
    machine = service.acquireStateMachine(machineId);
    machine.getExtendedState().getVariables().put("actions", new ArrayList<ActionSuit>());
    machine.getExtendedState().getVariables().put("deleteActions", new ArrayList<ActionSuit>());
    return machine;
  }

  public StateMachine<String, String> createMachine(JsonNode machineState) {
    String machineId = UUID.randomUUID().toString();
    return createMachine(machineState, machineId);
  }

  @Nested 
  class ContextConverter {
    @Test
    public void testContextBuilder() throws Exception {
      ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode machineState = mapper.readTree(resource.getInputStream());
      StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
      log.debug("context: {}", context);
      assertEquals("Provision", context.getState());
      assertEquals(3, context.getChilds().size());
      // assertThat(context.getExtendedState()).isNotNull();
    }

    @Test
    public void testJsonBuilder() throws Exception {
      ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode machineState = mapper.readTree(resource.getInputStream());
      String machineId = "testJsonBuilderSource";
      machine = createMachine(machineState, machineId);
      JsonNode machineStateTarget = QStateMachineContextConverter.toJsonNode(machine.getState());
      log.debug("expected: {}", machineState.toString());
      JSONAssert.assertEquals(machineState.toString(), machineState.toString(), false);
      JSONAssert.assertEquals(machineState.toString(), machineStateTarget.toString(), false);
    }

    // This test is just example how accessor work
    // Unfortunately it cause exception when accessing orthogonal regions
    // so we has gone to the persist/restore with context method
    @Test
    public void testSimpleStateSetByAccessor() throws Exception {
      machine = service.acquireStateMachine("testSimpleStateSet");
      var accessor = machine.getStateMachineAccessor();
      StateMachineContext<String, String> context = new DefaultStateMachineContext<String,String>("Aborted", null, null, null);
      Consumer<StateMachineAccess<String, String>> access;
      access = arg -> {
        arg.resetStateMachineReactively(context).block();
      }; 
      accessor.doWithAllRegions(access);

      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Aborted")
                .and()
            .build();
          plan.test();
    }

    @Test
    public void testSimpleStateSet() throws Exception {
      String machineId = "testSimpleStateSet";
      ObjectMapper mapper = new ObjectMapper();
      JsonNode machineState = mapper.readTree("\"Aborted\"");
      machine = createMachine(machineState, machineId);
      log.debug("machine: {}", machine);

      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectState("Aborted")
                .and()
            .build();
          plan.test();
    }

    @Test
    public void testOrthogonalStateSet() throws Exception {
      String machineId = "testOrthogonalStateSet";
      ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
      ObjectMapper mapper = new ObjectMapper();
      JsonNode machineState = mapper.readTree(resource.getInputStream());
      machine = createMachine(machineState, machineId);
      log.debug("machine: {}", machine);
      log.debug("states: {}", machine.getState().getIds());
      log.debug("search states: {}", Arrays.asList(stateSuit("Prolongation", "Paid", "PriceActive")));
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(stateSuit("Prolongation", "Paid", "PriceActive"))
                .and()
            .build();
          plan.test();
      assert(machine.getState().getIds().contains("Paid"));
    }

    @Test
    public void testOrthogonalStateSetMachineBuilder() throws Exception {
      String machineId = "testOrthogonalStateSetMachineBuilder";
      JsonNode machineState = Helper.buildMachineState("Prolongation", "Paid", "PriceActive");
      machine = createMachine(machineState, machineId);
      log.debug("machine: {}", machine);
      log.debug("states: {}", machine.getState().getIds());
      log.debug("search states: {}", Arrays.asList(stateSuit("Prolongation", "Paid", "PriceActive")));
      StateMachineTestPlan<String, String> plan =
          StateMachineTestPlanBuilder.<String, String>builder()
            .defaultAwaitTime(2)
            .stateMachine(machine)
            .step()
                .expectStates(stateSuit("Prolongation", "Paid", "PriceActive"))
                .and()
            .build();
          plan.test();
      assert(machine.getState().getIds().contains("Paid"));
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

  @Test
  public void testInitial() throws Exception {
    machine = createMachine(null);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .build();
        plan.test();
  }

  @Test
  public void testActivationStartedSuccess() throws Exception {
    machine = createMachine(null);
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-active").build();
    machine.getExtendedState().getVariables().put("product", product);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .step()
              .sendEvent(MessageBuilder.withPayload("activation_started")
                  .setHeader("product", product)
                  .setHeader("datetime", OffsetDateTime.now())
                  .build())
              .expectState("PendingActivate")
              .expectStateChanged(1)
              .and()
          .build();
    plan.test();
    assertEquals(product.getStatus(), "PENDING_ACTIVATE");
  }

  @Test
  public void testActivationCompletedSuccess() throws Exception {
    machine = createMachine(null, "testActivationCompletedSuccess");
    Product product = new ProductBuilder("simpleOffer1", "", "simple1-price-trial").build();
    ProductPrice price = product.getProductPrice().get(0);
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    ExtendedState extendedState = machine.getExtendedState();
    Map<Object, Object> variables = extendedState.getVariables();
    variables.put("product", product);
    List<ActionSuit> actions = (List<ActionSuit>) variables.get("actions");
    List<ActionSuit> deleteActions = (List<ActionSuit>) variables.get("deleteActions");

    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .step()
              .sendEvent("activation_started")
              .expectState("PendingActivate")
              .expectStateChanged(1)
              .and()
          .step()
              .sendEvent("activation_completed")
              .expectStates(stateSuit("ActiveTrial", "Paid", "PriceActive"))
              .and()
          .defaultAwaitTime(1)
          .build();
    plan.test();
    log.debug("states: {}", machine.getState().getIds());
    log.debug("actions: {}, deleteActions: {}", actions, deleteActions);
    assertEquals(product.getStatus(), "ACTIVE_TRIAL");
    assert(actions.contains(ActionSuit.PRICE_ENDED));
    assert(! actions.contains(ActionSuit.WAITING_PAY_ENDED));
    assert(! deleteActions.contains(ActionSuit.WAITING_PAY_ENDED));
  }
}
