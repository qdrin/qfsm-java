package org.qdrin.qfsm;

import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.StateMachine;
import org.qdrin.qfsm.machine.Events;
import org.qdrin.qfsm.machine.States;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	private StateMachine<States, Events> stateMachine;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Scanner in = new Scanner(System.in);
		String input = "disconnect";
		while(! input.equals("exit")) {
			System.out.print("input event name(exit to exit):");
			input = in.nextLine();
			try {
				Events event = Events.valueOf(input);
				log.info("Sending event: {}", input);
				stateMachine.sendEvent(event);
			} catch(IllegalArgumentException e) {
				log.info("'{}' is not valid event name. Try more", input);
				continue;
			}
		}
		log.info("exiting...");
		in.close();
	}
}
