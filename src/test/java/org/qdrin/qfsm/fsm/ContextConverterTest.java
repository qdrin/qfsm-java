package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.access.StateMachineAccess;

import static org.junit.Assert.*;

import java.util.function.Consumer;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class ContextConverterTest extends Helper {

  @Resource(name = "stateMachinePersist")
  private ProductStateMachinePersist persist;

  @Autowired
  StateMachineService<String, String> service;

  private StateMachine<String, String> machine;

  @BeforeEach
  public void setup() throws Exception {
    clearDb();
  }

  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testInitialState() throws Exception {
    machine = createMachine();
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
    machine = createMachine(machineState);
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
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree("\"Aborted\"");
    machine = createMachine(machineState);
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
    ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree(resource.getInputStream());
    machine = createMachine(machineState);
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
  public void testOrthogonalStateSetMachineStateBuilder() throws Exception {
    JsonNode machineState = buildMachineState("Prolongation", "Paid", "PriceActive");
    machine = createMachine(machineState);
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
  }

  @Test
  public void testMachineStateSequence() throws Exception {
    JsonNode machineState = buildMachineState("Prolongation", "Paid", "PriceActive");
    machine = createMachine(machineState);
    assertEquals("Provision", machine.getState().getId());
    machine = createMachine();
    assertEquals(machine.getState().getId(), "Entry");
  }
}
