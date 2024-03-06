package org.qdrin.qfsm;

import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.qdrin.qfsm.model.Product;
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

  // get stringified full-state
  public String getMachineState(State<String, String> state) {
		String mstate = state.getId();
		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState) state;
			// log.info("regions: {}", rstate.getRegions());
			mstate += "->[";
			for(var r: rstate.getRegions()) {
				// log.info("orthogonal region: {}, state: {}", r.getId(), r.getState().getId());
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

  public void sendUserEvent(String machineId, Scanner scanner) {
    StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
    String machineState = getMachineState(machine.getState());
    var variables = machine.getExtendedState().getVariables();
    log.info("current state: {}, variables: {}", machineState, variables);
    System.out.print("input event name:");
    String event = scanner.nextLine();
    sendEvent(machine, event);
    machineState = getMachineState(machine.getState());
    variables = machine.getExtendedState().getVariables();
    log.info("new state: {}, variables: {}", machineState, variables);
		// stateMachineService.releaseStateMachine(machineId);
  }

  public void sendEvent(StateMachine<String, String> machine, String eventName) throws IllegalArgumentException {
    // StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("transitionCount", 0);

		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.info("sending event: {}, message: {}", eventName, message);
    try {
		  machine.sendEvent(monomsg).blockLast();
    } catch(IllegalArgumentException e) {
      log.error("Event {} not accepted in current state: {}", eventName, e.getMessage());
    }

		int trcount = (int) machine.getExtendedState().getVariables().get("transitionCount");
		// Here we can distinguish accepted event from non-accepted
		if(trcount == 0) {
			log.error("Not transition triggered. Event vasted");
		} else {
			log.info("event processed, transitionCount={}", trcount);
		}
	}
}
