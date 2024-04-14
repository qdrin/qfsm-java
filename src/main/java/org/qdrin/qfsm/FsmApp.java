package org.qdrin.qfsm;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class FsmApp {

	// @Autowired
	// StateMachineFactory<String, String> stateMachineFactory;

	@Autowired
  	StateMachineService<String, String> stateMachineService;
	
	@Autowired
	StateMachinePersister<String, String, String> persister;

  // get stringified full-state
  public String getMachineState(State<String, String> state) {
		String mstate = state.getId();
		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState<String, String>) state;
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

	public <T> T getVariable(String machineId, String varname, Class<T> clazz) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		T var = machine.getExtendedState().get(varname, clazz);
		stateMachineService.releaseStateMachine(machineId);
		return var;
	}

	public void removeVariable(String machineId, String varname) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		machine.getExtendedState().getVariables().remove(varname);
		stateMachineService.releaseStateMachine(machineId);
	}

	public void setVariable(String machineId, String varname, Object var) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		machine.getExtendedState().getVariables().put(varname, var);
		stateMachineService.releaseStateMachine(machineId);
	}

//   public void sendUserEvent(String machineId) {
// 		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
//     String machineState = getMachineState(machine.getState());
//     var variables = machine.getExtendedState().getVariables();
//     log.info("current state: {}, variables: {}", machineState, variables);
//     System.out.print("input event name:");
//     String event = Application.scanner.nextLine();
//     sendEvent(machine, event);
//     machineState = getMachineState(machine.getState());
//     variables = machine.getExtendedState().getVariables();
//     log.info("new state: {}, variables: {}", machineState, variables);
// 		// stateMachineService.releaseStateMachine(machineId);
//   }

	public void sendEvent(String machineId, String event) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		try {
			persister.restore(machine, machineId);
			log.debug("machine.getId(): {}", machine.getId());
		} catch(Exception e) {
			log.error("Cannot restore stateMachineId '{}': {}", machineId, e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		String machineState = getMachineState(machine.getState());
		var variables = machine.getExtendedState().getVariables();
		log.info("current state: {}, variables: {}", machineState, variables);
		sendEvent(machine, event);
		machineState = getMachineState(machine.getState());
		variables = machine.getExtendedState().getVariables();
		log.info("new state: {}, variables: {}", machineState, variables);
			stateMachineService.releaseStateMachine(machineId);
	}

  public void sendEvent(StateMachine<String, String> machine, String eventName) throws IllegalArgumentException {
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
		stateMachineService.releaseStateMachine(machine.getId());
	}
}
