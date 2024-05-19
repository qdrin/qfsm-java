package org.qdrin.qfsm.fsm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.qdrin.qfsm.Helper;
import org.qdrin.qfsm.SpringStarter;
import org.qdrin.qfsm.BundleBuilder;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.service.QStateMachineContextConverter;
import static org.qdrin.qfsm.Helper.buildMachineState;
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

  private static Stream<Arguments> testJsonBuilder() {
    return Stream.of(
      Arguments.of("'PendingActivate'"),
      Arguments.of("'Disconnect'"),
      Arguments.of("{'Provision': [{'UsageRegion': 'PendingDisconnect'}, {'PaymentRegion': 'PaymentFinal'}, {'PriceRegion': 'PriceFinal'}]}"),
      Arguments.of("{'Provision': [{'UsageRegion': {'UsageOn': 'Suspended'}}, {'PaymentRegion': {'PaymentOn': 'NotPaid'}}, {'PriceRegion': {'PriceOn': 'PriceWaiting'}}]}"),
      Arguments.of("{'Provision': [{'UsageRegion': {'UsageOn': {'Activated': 'Active'}}}, {'PaymentRegion': {'PaymentOn': 'NotPaid'}}, {'PriceRegion': {'PriceOn': 'PriceWaiting'}}]}")
    );
  }
  @ParameterizedTest
  @MethodSource
  public void testJsonBuilder(String machineStateSample) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode machineState = mapper.readTree(machineStateSample.replace("'", "\""));
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .machineState(machineState)
      .build();
    machine = createMachine(bundle);
    log.debug("machine started. states: {}", machine.getState().getIds());
    JsonNode machineStateTarget = QStateMachineContextConverter.toJsonNode(machine.getState());
    releaseMachine(machine.getId());
    log.debug("expected: {}", machineState.toString());
    log.debug("actual:", machineStateTarget);
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

  @Test 
  public void fromStateContextFromStart() throws Exception {
    List<String> states = Arrays.asList("ActiveTrial", "Paid", "PriceActive");
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-trial")
      .priceNextPayDate(OffsetDateTime.now().plusDays(30))
      .build();
    machine = createMachine(bundle);

    StateMachineTestPlan<String, String> plan =
    StateMachineTestPlanBuilder.<String, String>builder()
      .defaultAwaitTime(2)
      .stateMachine(machine)
      .step()
          .expectState("Entry")
          .and()
      .step()
          .sendEvent("activation_started")
          .sendEvent("activation_completed")
          .expectStates(Helper.stateSuite(states))
          .and()
      .build();
    plan.test();
    JsonNode machineState = QStateMachineContextConverter.toJsonNode(machine.getState());
    releaseMachine(machine.getId());
    log.debug("machineState: {}", machineState);
    for(String state: states) {
      assert(machineState.toString().contains(state));
      if(states.size() == 3) assert(machineState.has("Provision"));
    }
  }

  private static Stream<Arguments> testFromStateContext() {
    return Stream.of(
      Arguments.of(Arrays.asList("Entry")),
      Arguments.of(Arrays.asList("Suspending", "NotPaid", "PriceWaiting")),
      Arguments.of(Arrays.asList("Active", "Paid", "PriceActive")),
      Arguments.of(Arrays.asList("PendingActivate")),
      Arguments.of(Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal")),
      Arguments.of(Arrays.asList("Suspending", "NotPaid", "PriceWaiting")),
      Arguments.of(Arrays.asList("Disconnect"))
    );
  }
  
  @ParameterizedTest
  @MethodSource
  public void testFromStateContext(List<String> states) throws Exception {
    TestBundle bundle = new BundleBuilder("simpleOffer1", "simple1-price-active")
      .machineState(buildMachineState(states))
      .build();
    machine = createMachine(bundle);
    List<String> expectedStates = Arrays.asList(Helper.stateSuite(states));
    for(String exp: expectedStates) {
      assert(machine.getState().getIds().contains(exp));
    }

    JsonNode machineState = QStateMachineContextConverter.toJsonNode(machine.getState());
    log.debug("machineState: {}", machineState);
    for(String state: states) {
      assert(machineState.toString().contains(state));
      if(states.size() == 3) assert(machineState.has("Provision"));
    }
  }

  private static Stream<Arguments> testBuildComponentMachineState() {
    return Stream.of(
      Arguments.of(Arrays.asList("Entry"), Arrays.asList("Entry")),
      Arguments.of(Arrays.asList("Disconnect"), Arrays.asList("Disconnect")),
      Arguments.of(Arrays.asList("PendingActivate"), Arrays.asList("PendingActivate")),
      Arguments.of(Arrays.asList("Suspending", "NotPaid", "PriceWaiting"), Arrays.asList("Suspending", "PaymentFinal", "PriceFinal")),
      Arguments.of(Arrays.asList("Active", "Paid", "PriceActive"), Arrays.asList("Active", "PaymentFinal", "PriceFinal")),
      Arguments.of(Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal"), Arrays.asList("PendingDisconnect", "PaymentFinal", "PriceFinal")),
      Arguments.of(Arrays.asList("Suspended", "NotPaid", "PriceWaiting"), Arrays.asList("Suspended", "PaymentFinal", "PriceFinal"))
    );
  }
  
  @ParameterizedTest
  @MethodSource
  public void testBuildComponentMachineState(List<String> states, List<String> expectedStates) throws Exception {
    JsonNode machineState = buildMachineState(states);
    for(String state: states) {
      assert(machineState.toString().contains(state));
    }
    JsonNode componentState = QStateMachineContextConverter.buildComponentMachineState(machineState);
    JsonNode expectedComponentState = buildMachineState(expectedStates);
    assertEquals(expectedComponentState, componentState);
  }
}
