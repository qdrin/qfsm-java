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
import org.qdrin.qfsm.model.ProductDescription;
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

    @PostMapping("/v1/event")
    public ResponseEntity<ResponseEventDto> eventHandler(@RequestBody @Valid RequestEventDto incomingEvent) {
			Event event = new Event(incomingEvent);
			List<ProductBundle> bundles = fsmApp.sendEvent(event);
			log.debug("bundles: {}", bundles);
			List<ProductDescription> productDescriptions = new ArrayList<>();
			List<ProductResponseDto> responseProducts = new ArrayList<>();
			for(ProductBundle bundle: bundles) {
				productDescriptions.add(bundle.getDrive());
				List<ProductDescription> components = bundle.getComponents(); 
				if(components != null) {
					productDescriptions.addAll(bundle.getComponents());
				}
			}
			log.debug("productDescriptions: {}", productDescriptions);
			for(ProductDescription desc: productDescriptions) {
				ProductResponseDto resp = new ProductResponseDto(desc.getProduct());
				if(event.getEventType().equals("activation_started")) {
					resp.setProductOrderItemId(desc.getProductOrderItemId());
				}
				responseProducts.add(resp);
			}
			ResponseEventDto response = new ResponseEventDto();
			response.setRefId(incomingEvent.getEvent().getRefId());
			response.setProducts(responseProducts);
	
			return ResponseEntity.ok(response);
    }
}

