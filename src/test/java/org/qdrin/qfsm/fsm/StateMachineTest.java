package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.access.StateMachineAccess;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.tasks.ActionSuit;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

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

  private StateMachineContext<String, String> createContext(JsonNode jstate) {
    StateMachineContext<String, String> context = null;
    ExtendedState estate = new DefaultExtendedState();
    String state = null;
    switch(jstate.getNodeType()) {
      case OBJECT:
        state = jstate.fieldNames().next();
        context = new DefaultStateMachineContext<>(state, null, null, estate);
        JsonNode jchild = jstate.get(state);
        if(jchild.getNodeType() == JsonNodeType.ARRAY) {
          for(var j: jchild) {
            StateMachineContext<String, String> child = createContext(j);
            context.getChilds().add(child);
          }
        } else {
          StateMachineContext<String, String> child = createContext(jchild);
          context.getChilds().add(child);
        }
        break;  // ?
      case STRING:
        state = jstate.asText();
        context = new DefaultStateMachineContext<>(state, null, null, estate);
      default:
    }
    return context;
  }

  public StateMachine<String, String> createMachine(JsonNode machineState, String machineId) {
    if(machineState != null) {
      try {
        StateMachineContext<String, String> context = createContext(machineState);
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

  @Test
  public void testContextBuilder() throws Exception {
    ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree(resource.getInputStream());
    StateMachineContext<String, String> context = createContext(machineState);
    log.debug("context: {}", context);
    assertEquals("Provision", context.getState());
    assertEquals(3, context.getChilds().size());
    // assertThat(context.getExtendedState()).isNotNull();
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

  // This test is just example how accessor work
  // Unfortunately it cause exception when accessing orthogonal regions
  // so we has gone to the persist/restore with context method
  @Test
  public void testSimpleStateSet() throws Exception {
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
  public void testOrthogonalStateSet() throws Exception {
    String machineId = "testOrthogonalStateSet";
    ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree(resource.getInputStream());
    machine = createMachine(machineState, machineId);
    log.debug("machine: {}", machine);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates("Prolongation", "Paid", "PriceActive")
              .and()
          .build();
        plan.test();
    log.debug("state: {}", machine.getState());
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
    machine.getExtendedState().getVariables().put("product", product);
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
    assertEquals(product.getStatus(), "ACTIVE_TRIAL");
  }
}
