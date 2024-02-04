package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.tasks.ExternalData;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private StateMachine<String, String> stateMachine;

	private String getMachineState(State<String, String> state) {
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

	private String getMachineState() {
		State<String, String> state = stateMachine.getState();
		return getMachineState(state);
	}

	private void sendUserEvent(String eventName) {
		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("now", OffsetDateTime.now())
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.info("sending event: {}, message: {}", eventName, message);
		if(eventName.equals("change_price") || eventName.equals("resume_completed")) {
			ProductPrice nextPrice = ExternalData.RequestProductPrice();
			stateMachine.getExtendedState().getVariables().put("nextPrice", nextPrice);
		} else {
			stateMachine.getExtendedState().getVariables().remove("nextPrice");
		}
		var evResult = stateMachine.sendEvent(monomsg).collectList();
		evResult.block();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner in = new Scanner(System.in);
		String input = "AAA";
		var runsm = stateMachine.startReactively();
		runsm.block();
		Map<Object, Object> machineVars = stateMachine.getExtendedState().getVariables();
		var price = ExternalData.RequestProductPrice();
		log.info("price: {}", price);
		machineVars.put("productPrice", price);
		var state = stateMachine.getState();
		String sname = (state == null) ? "null" : state.getId();
		log.info("initial state: {}", sname);
		while(! input.equals("exit")) {
			System.out.print("input event name(exit to exit):");
			input = in.nextLine();
			sendUserEvent(input);
			state = stateMachine.getState();
			sname = getMachineState();
			var variables = stateMachine.getExtendedState().getVariables();
			log.info("new state: {}, variables: {}", sname, variables);
		}
		log.info("exiting...");
		in.close();
		var stop_sm = stateMachine.stopReactively();
		stop_sm.block();
	}
}
