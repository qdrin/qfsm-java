package org.qdrin.qfsm.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.qdrin.qfsm.FsmApp;
import org.qdrin.qfsm.model.Event;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductBundle;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.qdrin.qfsm.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Service
@Slf4j
public class EventController {
    // @Autowired
    // StateMachineService<String, String> stateMachineService;
  
    // @Autowired
    // StateMachinePersister<String, String, String> persister;
    
    // @Autowired
    // DataSource dataSource;

		@Autowired
		FsmApp fsmApp;

    // private void sendEvent(RequestActivateEventDto activateEvent) {
        // String machineId = UUID.randomUUID().toString();
		// StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		// try {
		// 	persister.restore(machine, machineId);
		// 	log.debug("machine.getId(): {}", machine.getId());
		// } catch(Exception e) {
		// 	log.error("Cannot restore stateMachineId '{}': {}", machineId, e.getLocalizedMessage());
		// 	e.printStackTrace();
		// 	return;
		// }
		// String machineState = getMachineState(machine.getState());
		// var variables = machine.getExtendedState().getVariables();
		// log.info("current state: {}, variables: {}", machineState, variables);
		// sendEvent(machine, event);
		// machineState = getMachineState(machine.getState());
		// variables = machine.getExtendedState().getVariables();
		// log.info("new state: {}, variables: {}", machineState, variables);
		// 	stateMachineService.releaseStateMachine(machineId);
	// }

    @PostMapping("/v1/event")
    public ResponseEntity<ResponseEventDto> eventHandler(@RequestBody @Valid RequestEventDto incomingEvent) {
			Event event = new Event(incomingEvent);
			List<ProductBundle> bundles = fsmApp.sendEvent(event);
			List<ProductResponseDto> responseProducts = new ArrayList<>();
			for(Product product: products) {
				responseProducts.add(new ProductResponseDto(product));
			}
			ResponseEventDto response = new ResponseEventDto();
			response.setRefId(incomingEvent.getEvent().getRefId());
			response.setProducts(responseProducts);
	
			return ResponseEntity.ok(response);
    }
}

