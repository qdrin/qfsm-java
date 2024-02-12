package org.qdrin.qfsm;

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
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;


@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private StateMachine<String, String> stateMachine;

	@Autowired
	private StateMachinePersister<String, String, String> stateMachinePersister;

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

	private void sendUserEvent(String machineId, String eventName) throws IllegalArgumentException {
		stateMachine.getExtendedState().getVariables().put("transitionCount", 0);
		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.info("sending event: {}, message: {}", eventName, message);
		stateMachine.sendEvent(monomsg).blockLast();
		
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
		String mid = "1";
		var runsm = stateMachine.startReactively();
		runsm.block();
		while(! input.equals("exit")) {
			System.out.print("input machineId:");
			mid = in.nextLine();
			System.out.print("input event name(exit to exit):");
			input = in.nextLine();
			try {
				stateMachinePersister.restore(stateMachine, mid);
				sendUserEvent(mid, input);
				State<String, String> state = stateMachine.getState();
				String machineState = getMachineState();
				var variables = stateMachine.getExtendedState().getVariables();
				log.info("new state: {}, variables: {}", machineState, variables);
				stateMachinePersister.persist(stateMachine, mid);
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
