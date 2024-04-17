package org.qdrin.qfsm.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.UUID;

import javax.sql.DataSource;

import org.qdrin.qfsm.FsmApp;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
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
    public ResponseEntity<ResponseEventDto> eventHandler(@RequestBody @Valid RequestEventDto activateEvent) {
			String machineId = UUID.randomUUID().toString();
			ProductActivateRequestDto orderItem = activateEvent.getProductOrderItems().get(0);
			ResponseEventDto response = new ResponseEventDto();
			response.setRefId(activateEvent.getEvent().getRefId());
			ProductResponseDto product = new ProductResponseDto();
			product.setProductId(machineId);
			product.setProductOfferingId(orderItem.getProductOfferingId());
			product.setBundle(orderItem.getIsBundle());
			response.setProducts(Arrays.asList(product));
			fsmApp.sendEvent(machineId, activateEvent.getEvent().getEventType());
			return ResponseEntity.ok(response);
    }
}

