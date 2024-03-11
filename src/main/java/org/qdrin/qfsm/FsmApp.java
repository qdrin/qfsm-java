package org.qdrin.qfsm;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.entity.ProductEntity;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class FsmApp {

  private StateMachine<String, String> stateMachine;

  @Autowired
	private StateMachineService<String, String> stateMachineService;

	@Autowired
	ProductRepository productRepository;

	// @Autowired
	// TestRepository testRepository;

	private void saveVariables(StateMachine<String, String> machine) {
		String machineId = machine.getId();
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		Product product = ((Product) variables.get("product"));
		if(product != null && product.getProductId().isEmpty()) {
			product.setProductId(machineId);
		}
		productRepository.save(product);
		variables.remove("product");
	}

	private void restoreVariables(StateMachine<String, String> machine) {
		String machineId = machine.getId();
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		Optional<Product> product = productRepository.findById(machineId);
		if(! product.isEmpty()) {
			variables.put("product", product.get());
		}
	}

  // get stringified full-state
  public String getMachineState(State<String, String> state) {
		String mstate = state.getId();
		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState) state;
			mstate += "->[";
			for(var r: rstate.getRegions()) {
				mstate += getMachineState(r.getState()) + ",";
			}
			mstate = mstate.substring(0, mstate.length()-1) + "]";
		}
		if(state.isSubmachineState()) {
			StateMachine<String, String> submachine = ((AbstractState<String, String>) state).getSubmachine();
			State<String, String> sstate = submachine.getState();
			mstate = mstate + "->" + getMachineState(sstate);
		}
		return mstate;
	}

	private synchronized StateMachine<String, String> getStateMachine(String machineId) throws Exception {
		if (stateMachine == null) {
			stateMachine = stateMachineService.acquireStateMachine(machineId);
			log.debug("getStateMachine created stateMachine: {}", machineId);
			stateMachine.startReactively().block();
		} else if (!ObjectUtils.nullSafeEquals(stateMachine.getId(), machineId)) {
			String oldId = stateMachine.getId();
			stateMachineService.releaseStateMachine(stateMachine.getId());
			log.debug("getStateMachine released stateMachine: {}", oldId);
			stateMachine = stateMachineService.acquireStateMachine(machineId);
		}
		log.debug("getStateMachine acquired machineId: {}, state: {}, variables: {}",
							machineId,
							getMachineState(stateMachine.getState()),
							stateMachine.getExtendedState().getVariables());
		return stateMachine;
	}

  public void sendUserEvent(String machineId, Scanner scanner) {
		StateMachine<String, String> machine;
		try {
    	machine = getStateMachine(machineId);
		}
		catch(Exception e) {
			log.error("Cannot acquire stateMachineId '{}': {}", machineId, e.getLocalizedMessage());
			return;
		}
    System.out.print("input event name:");
    String event = scanner.nextLine();
    sendEvent(machine, event);
    String machineState = getMachineState(machine.getState());
    Map<Object, Object> variables = machine.getExtendedState().getVariables();
    log.info("new state: {}, variables: {}", machineState, variables);
  }

  public void sendEvent(StateMachine<String, String> machine, String eventName) throws IllegalArgumentException {
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("transitionCount", 0);

		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.info("sending event: {}, machine: {}", eventName, machine.getId());
    try {
			restoreVariables(machine);
		  machine.sendEvent(monomsg).blockLast();
			saveVariables(machine);
    } catch(IllegalArgumentException e) {
      log.error("Event {} not accepted in current state: {}", eventName, e.getMessage());
    }

		int trcount = (int) machine.getExtendedState().getVariables().get("transitionCount");
		// Here we can distinguish accepted event from non-accepted
		if(trcount == 0) {
			log.warn("No transition triggered. Event vasted");
		}
	}
}
