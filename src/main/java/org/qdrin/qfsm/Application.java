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
import org.springframework.statemachine.state.AbstractState;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private StateMachine<String, String> stateMachine;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner in = new Scanner(System.in);
		String input = "AAA";
		var runsm = stateMachine.startReactively();
		runsm.block();
		var state = stateMachine.getState();
		String sname = (state == null) ? "null" : state.getId();
		log.info("state: {}, isSubmachineState: {}", sname, state.isSubmachineState());
		System.out.println("checking System.out.println. OK");
		while(! input.equals("exit")) {
			log.info("input event name(exit to exit):");
			input = in.nextLine();
			try {
				var state0 = stateMachine.getState();
				String sname0 = (state0 == null) ? "null" : state0.getId();
				log.info("current state: {}, sending event: {}", sname0, input);
				var states = state0.getStates();
				Mono<Message<String>> msg = Mono.just(MessageBuilder
					.withPayload(input).build());
				var evResult = stateMachine.sendEvent(msg).collectList();
				evResult.block();
				state = stateMachine.getState();
				sname = (state == null) ? "null" : state.getId();
				String subId = "Null";
				if(state.isSubmachineState()) {
					var submachine = ((AbstractState<String, String>) state).getSubmachine();
					sname = String.format("%s->%s", sname, submachine.getState().getId());
				}
				log.info("new state: {}", sname);
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
