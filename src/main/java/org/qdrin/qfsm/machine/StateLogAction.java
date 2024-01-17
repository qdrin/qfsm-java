package org.qdrin.qfsm.machine;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateLogAction implements Action<String, String> {
		@Override
		public void execute(StateContext<String, String> context) {
			log.info("StateLogAction: {}", context.getTarget().getId());
		}
}
