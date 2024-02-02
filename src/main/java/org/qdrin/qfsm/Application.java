package org.qdrin.qfsm;

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

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private StateMachine<String, String> stateMachine;

	private Price getPriceFromConsole() {
		Scanner in = new Scanner(System.in);
		Price price = new Price();
		System.out.println("Input price attributes");
		System.out.println("priceId:");
		price.setPriceId(in.nextLine());
		System.out.println("productStatus[ACTIVE/ACTIVE_TRIAL]:");
		price.setProductStatus(in.nextLine());
		System.out.println("duration");
		price.setDuration(Integer.valueOf(in.nextLine()));
		return price;
	}

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
		var price = getPriceFromConsole();
		machineVars.put("productStatus", price.getProductStatus());
		machineVars.put("price", price);
		var state = stateMachine.getState();
		String sname = (state == null) ? "null" : state.getId();
		log.info("initial state: {}", sname);
		while(! input.equals("exit")) {
			log.info("input event name(exit to exit):");
			input = in.nextLine();
			try {
				var state0 = stateMachine.getState();
				log.info("sending event: {}", input);
				Mono<Message<String>> msg = Mono.just(MessageBuilder
					.withPayload(input).build());
				var evResult = stateMachine.sendEvent(msg).collectList();
				evResult.block();
				state = stateMachine.getState();
				sname = getMachineState();
				var variables = stateMachine.getExtendedState().getVariables();
				log.info("new state: {}, variables: {}", sname, variables);
			} catch(IllegalArgumentException e) {
				log.info("'{}' is not valid event name. Try more", input);
				continue;
			}
		}
		log.info("exiting...");
		in.close();
		var stop_sm = stateMachine.stopReactively();
		stop_sm.block();
	}
}
