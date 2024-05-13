package org.qdrin.qfsm.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.*;
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
		return acquireStateMachine(product, null, new ArrayList<Product>());
	}

	public StateMachine<String, String> acquireStateMachine(Product product, Product bundle) {
		return acquireStateMachine(product, bundle, new ArrayList<Product>());
	}
		
	public StateMachine<String, String> acquireStateMachine(Product product, Product bundle, List<Product> components) {
		String machineId = product.getProductId();
		JsonNode machineState = product.getMachineContext().getMachineState();
		log.debug("Acquiring machineId: {}, machineState: {}", machineId, machineState);

		StateMachine<String, String> machine = stateMachineFactory.getStateMachine(machineId);
		if(machineState != null) {
			StateMachineContext<String, String> context = QStateMachineContextConverter.toContext(machineState);
			machine.getStateMachineAccessor().doWithAllRegions(
				function -> function.resetStateMachineReactively(context).block()
			);
			((AbstractStateMachine<String, String>) machine).setId(machineId);
		}
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("tasks", new TaskPlan(product.getProductId()));
		variables.put("product", product);
		if(bundle != null) {
			variables.put("bundle", bundle);
		}
		if(components == null) {
			components = new ArrayList<>();
		}
		variables.put("components", components);
		machine.startReactively().block();
		machines.put(machineId, machine);
		return machine;
	}

	public void releaseStateMachine(String machineId) {
		log.info("Releasing machine with id " + machineId);
		synchronized (machines) {
			StateMachine<String, String> stateMachine = machines.remove(machineId);
			Product product = stateMachine.getExtendedState().get("product", Product.class);
			product.getMachineContext().setMachineState(QStateMachineContextConverter.toJsonNode(stateMachine.getState()));
		}
	}

	public boolean hasStateMachine(String machineId) {
		synchronized (machines) {
			return machines.containsKey(machineId);
		}
	}
}
