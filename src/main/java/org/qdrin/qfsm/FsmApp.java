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
import org.qdrin.qfsm.exception.*;

@Slf4j
@Configuration
public class FsmApp {

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

	public void sendEvent(String machineId, String event) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		log.debug("machine acquired: {}", machine.getId());
		String machineState = getMachineState(machine.getState());
		var variables = machine.getExtendedState().getVariables();
		log.info("current state: {}, variables: {}", machineState, variables);
		sendMessage(machine, event);
		machineState = getMachineState(machine.getState());
		variables = machine.getExtendedState().getVariables();
		log.info("new state: {}, variables: {}", machineState, variables);
		stateMachineService.releaseStateMachine(machineId);
	}

  private void sendMessage(StateMachine<String, String> machine, String eventName) {
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("transitionCount", 0);

		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.debug("[{}] sending event: {}, message: {}", machine.getId(), eventName, message);
    try {
		  var res = machine.sendEvent(monomsg).blockLast();
			log.debug("event result: {}", res);
    } catch(IllegalArgumentException e) {
			e.printStackTrace();
			String emsg = String.format("[%s] Event %s not accepted in current state: %s", machine.getId(), eventName, e.getLocalizedMessage());
      log.error(emsg);
			throw new NotAcceptedEventException(emsg, e);
    }
		finally {
			stateMachineService.releaseStateMachine(machine.getId());
		}

		int trcount = (int) machine.getExtendedState().getVariables().get("transitionCount");
		if(trcount == 0) {
			String emsg = String.format("[%s] Event %s not accepted in current state, transition count is zero", machine.getId(), eventName);
      log.error(emsg);
			throw new NotAcceptedEventException(emsg);
		}
	}
}
