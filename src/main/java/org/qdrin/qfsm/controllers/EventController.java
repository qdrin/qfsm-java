package org.qdrin.qfsm.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.FsmApp;
import org.qdrin.qfsm.model.Event;
import org.qdrin.qfsm.model.FsmResult;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductBundle;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductResponseDto;
import org.qdrin.qfsm.model.dto.RequestEventDto;
import org.qdrin.qfsm.model.dto.ResponseEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
			FsmResult result = fsmApp.sendEvent(event);
			List<ProductBundle> bundles = result.getBundles();
			List<ProductActivateRequestDto> orderItems = result.getProductOrderItems();
			log.debug("bundles: {}", bundles);
			List<Product> products = new ArrayList<>();
			List<ProductResponseDto> responseProducts = new ArrayList<>();
			for(ProductBundle bundle: bundles) {
				products.add(bundle.getDrive());
				List<Product> components = bundle.getComponents(); 
				if(components != null) {
					products.addAll(bundle.getComponents());
				}
			}
			log.debug("products: {}", products);
			for(Product product: products) {
				ProductResponseDto resp = new ProductResponseDto(product);
				if(event.getEventType().equals("activation_started")) {
					Optional<ProductActivateRequestDto> orderItem = orderItems
							.stream()
							.filter(item -> item.getProductId().equals(product.getProductId()))
							.findFirst();
					if(orderItem.isEmpty()) {
						throw new RuntimeException(String.format("Cannot find orderItem for productId: %s, productOfferingId: %s",
											product.getProductId(), product.getProductOfferingId()));
					}
					resp.setProductOrderItemId(orderItem.get().getProductOrderItemId());
				}
				responseProducts.add(resp);
			}
			ResponseEventDto response = new ResponseEventDto();
			response.setRefId(incomingEvent.getEvent().getRefId());
			response.setProducts(responseProducts);
	
			return ResponseEntity.ok(response);
    }
}

