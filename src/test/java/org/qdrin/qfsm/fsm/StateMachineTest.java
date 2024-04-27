package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.access.StateMachineAccess;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.ProductBuilder;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.service.StateMachineService;
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

  @Autowired
  private StateMachineFactory<String, String> stateMachineFactory;

  @Resource(name = "stateMachinePersist")
  private ProductStateMachinePersist persist;

  @Autowired
  StateMachineService<String, String> service;

  private StateMachine<String, String> machine;

  @BeforeEach
  public void setup() throws Exception {
    // machine = stateMachineFactory.getStateMachine();
  }

  ObjectMapper mapper = new ObjectMapper();

  private StateMachineContext<String, String> createContext(JsonNode jstate) {
    StateMachineContext<String, String> context = null;
    String state = null;
    switch(jstate.getNodeType()) {
      case OBJECT:
        state = jstate.fieldNames().next();
        context = new DefaultStateMachineContext<>(state, null, null, null);
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
        context = new DefaultStateMachineContext<>(state, null, null, null);
      default:
    }
    return context;
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
  }

  @Test
  public void testInitial() throws Exception {
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
  public void testSimpleStateSet() throws Exception {
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
    StateMachineContext<String, String> context = createContext(machineState);
    persist.write(context, machineId);
    machine = service.acquireStateMachine(machineId);
    // var accessor = machine.getStateMachineAccessor();
    // log.debug("context: {}", context);
    // Consumer<StateMachineAccess<String, String>> access;
    // access = arg -> {
    //   arg.resetStateMachineReactively(context).block();
    // }; 
    // accessor.doWithAllRegions(access);

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
}
