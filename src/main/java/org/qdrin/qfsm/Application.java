package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.Arrays;
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

	private void sendUserEvent(String eventName) throws IllegalArgumentException {
		stateMachine.getExtendedState().getVariables().put("transitionCount", 0);
		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.info("sending event: {}, message: {}", eventName, message);
		var evResult = stateMachine.sendEvent(monomsg).collectList();
		evResult.block();
		int trcount = (int) stateMachine.getExtendedState().getVariables().get("transitionCount");
		// Here we can distinguish accepted event from non-accepted
		if(trcount == 0) {
			log.error("Not transition triggered. Event vasted");
		} else {
			log.info("event processed, transitionCount={}", trcount);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner in = new Scanner(System.in);
		String input = "AAA";
		Map<Object, Object> machineVars = stateMachine.getExtendedState().getVariables();
		Product product = new Product();
		ProductPrice price = ExternalData.RequestProductPrice();
		log.info("price: {}", price);
		product.setProductPrices(Arrays.asList(price));
		machineVars.put("product", product);
		var runsm = stateMachine.startReactively();
		runsm.block();
		var state = stateMachine.getState();
		log.info("initial state: {}, variables: {}", state.getId(), machineVars);
		while(! input.equals("exit")) {
			System.out.print("input event name(exit to exit):");
			input = in.nextLine();
			try {
				sendUserEvent(input);
				state = stateMachine.getState();
				String machineState = getMachineState();
				var variables = stateMachine.getExtendedState().getVariables();
				log.info("new state: {}, variables: {}", machineState, variables);
			} catch(IllegalArgumentException e) {
				log.error("Event {} not accepted in current state: {}", input, e.getMessage());
			}
		}
		log.info("exiting...");
		in.close();
		var stop_sm = stateMachine.stopReactively();
		stop_sm.block();
	}
}
