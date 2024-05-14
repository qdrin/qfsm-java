package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.service.QStateMachineContextConverter;
import static org.qdrin.qfsm.service.QStateMachineContextConverter.buildMachineState;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextConverterTest extends SpringStarter {

  private StateMachine<String, String> machine;

  @BeforeEach
  public void setup() throws Exception {
    clearDb();
  }

  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testInitialState() throws Exception {
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-trial").build();
    machine = createMachine(bundle);
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectState("Entry")
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
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
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .machineState(machineState)
      .build();
    machine = createMachine(bundle);
    JsonNode machineStateTarget = QStateMachineContextConverter.toJsonNode(machine.getState());
    log.debug("expected: {}", machineState.toString());
    JSONAssert.assertEquals(machineState.toString(), machineState.toString(), false);
    JSONAssert.assertEquals(machineState.toString(), machineStateTarget.toString(), false);
  }

  @Test
  public void testSimpleStateSet() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree("\"Aborted\"");
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .machineState(machineState)
      .build();
    machine = createMachine(bundle);
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
    releaseMachine(machine.getId());
  }

  @Test
  public void testOrthogonalStateSet() throws Exception {
    ClassPathResource resource = new ClassPathResource("/contexts/machinestate_sample.json", getClass());
    ObjectMapper mapper = new ObjectMapper();
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active").build();
    JsonNode machineState = mapper.readTree(resource.getInputStream());
    bundle.drive.getMachineContext().setMachineState(machineState);
    machine = createMachine(bundle);
    log.debug("machine: {}", machine);
    log.debug("states: {}", machine.getState().getIds());
    log.debug("search states: {}", Arrays.asList(Helper.stateSuite("Prolongation", "Paid", "PriceActive")));
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite("Prolongation", "Paid", "PriceActive"))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
    assert(machine.getState().getIds().contains("Paid"));
  }

  @Test
  public void testOrthogonalStateSetMachineStateBuilder() throws Exception {
    JsonNode machineState = buildMachineState("Prolongation", "Paid", "PriceActive");
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active").build();
    bundle.drive.getMachineContext().setMachineState(machineState);
    machine = createMachine(bundle);
    log.debug("machine: {}", machine);
    log.debug("states: {}", machine.getState().getIds());
    log.debug("search states: {}", Arrays.asList(Helper.stateSuite("Prolongation", "Paid", "PriceActive")));
    StateMachineTestPlan<String, String> plan =
        StateMachineTestPlanBuilder.<String, String>builder()
          .defaultAwaitTime(2)
          .stateMachine(machine)
          .step()
              .expectStates(Helper.stateSuite("Prolongation", "Paid", "PriceActive"))
              .and()
          .build();
    plan.test();
    releaseMachine(machine.getId());
  }

  @Test
  public void testMachineStateSequence() throws Exception {
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .machineState(buildMachineState("Prolongation", "Paid", "PriceActive"))
      .build();
    machine = createMachine(bundle);
    assertEquals("Provision", machine.getState().getId());
    bundle = new BundleBuilder("simpleOffer1", "simple1-price-active").build();
    machine.stopReactively().block();
    machine = createMachine(bundle);
    log.debug("2nd stage. machine: {}, state: {}", machine, machine.getState());
    assertEquals("Entry", machine.getState().getId());
  }
}
