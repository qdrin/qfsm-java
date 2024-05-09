package org.qdrin.qfsm.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.persist.QStateMachineContextConverter;
import org.qdrin.qfsm.tasks.ActionSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.AbstractStateMachine;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QStateMachineService {

	// private final static Log log = LogFactory.getLog(QStateMachineService.class);
	@Autowired
	private StateMachineFactory<String, String> stateMachineFactory;
	private final Map<String, StateMachine<String, String>> machines = new HashMap<String, StateMachine<String, String>>();

	// public QStateMachineService(StateMachineFactory<String, String> stateMachineFactory) {
	// 	this.stateMachineFactory = stateMachineFactory;
	// }

	public StateMachine<String, String> acquireStateMachine(Product product) {
		String machineId = product.getProductId();
		JsonNode machineState = product.getMachineState();
		log.debug("Acquiring machineId: {}, machineState: {}", machineId, machineState);

		StateMachine<String, String> machine = stateMachineFactory.getStateMachine(machineId);
		log.debug("id0: {}", machine.getId());
		if(machineState != null) {
			StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
			machine.getStateMachineAccessor().doWithAllRegions(
				function -> function.resetStateMachineReactively(context).block()
			);
			((AbstractStateMachine<String, String>) machine).setId(machineId);
			log.debug("id1: {}", machine.getId());
		}
		log.debug("id2: {}", machine.getId());
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("actions", new ArrayList<ActionSuite>());
		variables.put("deleteActions", new ArrayList<ActionSuite>());
		variables.put("product", product);
		machine.startReactively().block();
		machines.put(machineId, machine);
		return machine;
	}

	public void releaseStateMachine(String machineId) {
		log.info("Releasing machine with id " + machineId);
		synchronized (machines) {
			StateMachine<String, String> stateMachine = machines.remove(machineId);
			Product product = stateMachine.getExtendedState().get("product", Product.class);
			product.setMachineState(QStateMachineContextConverter.toJsonNode(stateMachine.getState()));
			log.debug("releasing machine with product.machineState: {}", product.getMachineState());
			if (stateMachine != null) {
				log.info("Stoping machineId: {}", machineId);
				stateMachine.stopReactively().block();
			}
		}
	}

	public boolean hasStateMachine(String machineId) {
		synchronized (machines) {
			return machines.containsKey(machineId);
		}
	}
}
