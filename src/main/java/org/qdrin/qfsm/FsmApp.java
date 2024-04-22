package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.qdrin.qfsm.exception.*;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.repository.EventRepository;
import org.qdrin.qfsm.repository.ProductRepository;
import org.qdrin.qfsm.ProductClasses;

@Slf4j
@Configuration
public class FsmApp {

	@Autowired
  	StateMachineService<String, String> stateMachineService;
	
	@Autowired
	StateMachinePersister<String, String, String> persister;

	@Autowired
	EventRepository eventRepository;

	@Autowired
	ProductRepository productRepository;

  // get stringified full-state
  public String getMachineState(State<String, String> state) {
		String mstate = state.getId();
		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState<String, String>) state;
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

	public <T> T getVariable(String machineId, String varname, Class<T> clazz) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		T var = machine.getExtendedState().get(varname, clazz);
		stateMachineService.releaseStateMachine(machineId);
		return var;
	}

	public void removeVariable(String machineId, String varname) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		machine.getExtendedState().getVariables().remove(varname);
		stateMachineService.releaseStateMachine(machineId);
	}

	public void setVariable(String machineId, String varname, Object var) {
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		machine.getExtendedState().getVariables().put(varname, var);
		stateMachineService.releaseStateMachine(machineId);
	}

	private void checkEvent(Event event) {
		Event savedEvent = eventRepository.findByRefIdAndSourceCodeAndEventType(
			event.getRefId(),
			event.getSourceCode(),
			event.getEventType());
		// log.debug("savedEvent: {}", savedEvent);
		if(savedEvent != null) {
			String errString = String.format("Event is already processed. refId: %s, sourceCode: %s, eventType: %s",
						event.getRefId(), event.getSourceCode(), event.getEventType());
			log.error(errString);
			throw new RepeatedEventException(errString);
		}
	}

	private void validateEvent(Event event) {
		return;
	}

	private List<ProductBundle> createProducts(Event event) {
		ArrayList<ProductBundle> bundles = new ArrayList<>();
		List<ProductActivateRequestDto> orderItems = new ArrayList<ProductActivateRequestDto>(event.getProductOrderItems());
		log.debug("event: {}", event);

		List<ProductActivateRequestDto> heads = orderItems.stream()
				.filter(item -> item.getIsBundle())
				.collect(Collectors.toList());
		
		for(ProductActivateRequestDto head: heads) {
			ProductClasses productClass = head.getIsCustom() ? ProductClasses.CUSTOM_BUNDLE : ProductClasses.BUNDLE;
			ProductBundle bundle = new ProductBundle();
			Product product = new Product(head);
			product.setProductClass(productClass.ordinal());
			ArrayList<Product> components = new ArrayList<>();
			List<ProductRelationship> productRelations = new ArrayList<>(); 
			List<ProductOrderItemRelationshipDto> itemRelations = head.getProductOrderItemRelationship();
			for(ProductOrderItemRelationshipDto rel: itemRelations) {
				Optional<ProductActivateRequestDto> componentItem = orderItems.stream()
						.filter(item -> item.getProductOrderItemId().equals(rel.getProductOrderItemId()))
						.findFirst();
				if(componentItem.isEmpty()) {
					throw new BadUserDataException(
						String.format("Cannot find leg orderItem %s, specified in relations for %s", 
													rel.getProductOrderItemId(), head.getProductOrderItemId())
						);
				}
				Product component = new Product(componentItem.get());
				components.add(component);
				ProductRelationship pr = new ProductRelationship();
				pr.setProductId(component.getProductId());
				pr.setProductOfferingId(component.getProductOfferingId());
				pr.setRelationshipType(rel.getRelationshipType());
				pr.setProductOfferingName(component.getProductOfferingName());
				productRelations.add(pr);
			}
			product.setProductRelationship(productRelations);
			bundle.setMainProduct(product);
			bundle.setComponents(components);
			orderItems.remove(head);
		}  // End of bundle processing, orderItems contain just simple and legs now

		for(ProductActivateRequestDto orderItem: orderItems) {
			log.debug("productOrderItem {}", orderItem);
			ProductBundle simple = new ProductBundle();
			Product product = new Product(orderItem);
			List<ProductOrderItemRelationshipDto> relations = orderItem.getProductOrderItemRelationship();
			Optional<ProductOrderItemRelationshipDto> headRelation = relations.stream().filter(r -> r.getRelationshipType().equals("BELONGS")).findFirst();
			ProductClasses productClass = headRelation.isPresent() ? ProductClasses.CUSTOM_BUNDLE_COMPONENT : ProductClasses.SIMPLE;
			product.setProductClass(productClass.ordinal());
			simple.setMainProduct(product);
			simple.setComponents(null);
			bundles.add(simple);
		}
		log.debug("bundles: {}", bundles);
		return bundles;
	}

	private List<ProductBundle> getProducts(Event event) {
		ArrayList<ProductBundle> bundles = new ArrayList<>();
		List<ProductRequestDto> orderItems = new ArrayList<ProductRequestDto>(event.getProducts());
		ArrayList<Product> products = new ArrayList<>();
		log.debug("event: {}", event);
		for(ProductRequestDto orderItem: orderItems) {
			Optional<Product> dbProduct = productRepository.findById(orderItem.getProductId());
			if(dbProduct.isEmpty()) {
				String errString = String.format("Event contains productId: %s, that cannot be found");
				log.error( errString);
				throw new BadUserDataException(errString);
			}
			Product product = dbProduct.get();
			product.updateUserData(orderItem);
			products.add(product);
		}
		List<Product> heads = products.stream()
				.filter(p -> ProductClasses.getBundles().contains(p.getProductClass()))
				.collect(Collectors.toList());
		for(Product head: heads) {
			ProductBundle bundle = new ProductBundle();
			bundle.setMainProduct(head);
			List<Product> components = new ArrayList<>();
			for(ProductRelationship relationship: head.getProductRelationship()) {
				Optional<Product> component = products.stream().filter(p->p.getProductId().equals(relationship.getProductId())).findFirst();
				if(component.isEmpty()) {
					component = productRepository.findById(relationship.getProductId());
				} else {
					products.remove(component.get());
				}
				if(component.isEmpty()) {
					throw new RuntimeException(String.format("component %s not found for bundle %s",
										relationship.getProductId(), head.getProductId()));
				}
				components.add(component.get());
			}
			bundle.setComponents(components);
			bundles.add(bundle);
			products.remove(head);
		}
		for(Product product: products) {
			int index = product.getProductClass();
			ProductClasses productClass = ProductClasses.values()[index];
			switch(productClass) {
				case BUNDLE_COMPONENT:
					String errString = String.format("Hard bundle component without bundle: %s", product.getProductId());
					log.error(errString);
					throw new BadUserDataException(errString);
				case SIMPLE:
				case CUSTOM_BUNDLE_COMPONENT:
					ProductBundle bundle = new ProductBundle();
					bundle.setMainProduct(product);
					bundles.add(bundle);
					break;
				default:
					errString = String.format("Unexpected bundle or custom bundle remains after their processing: %s", product.getProductId());
					log.error(errString);
					throw new RuntimeException(errString);
			}
		}
		return bundles;
	}

	public List<ProductBundle> sendEvent(Event event) {
		checkEvent(event);
		validateEvent(event);
		List<ProductBundle> bundles = event.getEventType().equals("activation_started") ? createProducts(event) : getProducts(event);
		if(bundles.size() != 1) {
			String errString = String.format("incorrect number of products: %d", bundles.size());
			log.error(errString);
			throw new BadUserDataException(errString);
		}
		String machineId = bundles.get(0).getMainProduct().getProductId();
		StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
		String eventType = event.getEventType();
		log.debug("machine acquired: {}", machine.getId());
		String machineState = getMachineState(machine.getState());
		var variables = machine.getExtendedState().getVariables();
		log.info("current state: {}, variables: {}", machineState, variables);
		sendMessage(machine, eventType);
		machineState = getMachineState(machine.getState());
		variables = machine.getExtendedState().getVariables();
		log.info("new state: {}, variables: {}", machineState, variables);
		stateMachineService.releaseStateMachine(machineId);
		eventRepository.save(event);
		return bundles;
	}

  private void sendMessage(StateMachine<String, String> machine, String eventName) {
		Map<Object, Object> variables = machine.getExtendedState().getVariables();
		variables.put("transitionCount", 0);

		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.debug("[{}] sending event: {}, message: {}", machine.getId(), eventName, message);
    try {
		  var res = machine.sendEvent(monomsg).blockLast();
			log.debug("event result: {}", res);
    } catch(IllegalArgumentException e) {
			e.printStackTrace();
			String emsg = String.format("[%s] Event %s not accepted in current state: %s", machine.getId(), eventName, e.getLocalizedMessage());
      log.error(emsg);
			throw new NotAcceptedEventException(emsg, e);
    }
		finally {
			stateMachineService.releaseStateMachine(machine.getId());
		}

		int trcount = (int) machine.getExtendedState().getVariables().get("transitionCount");
		if(trcount == 0) {
			String emsg = String.format("[%s] Event %s not accepted in current state, transition count is zero", machine.getId(), eventName);
      log.error(emsg);
			throw new NotAcceptedEventException(emsg);
		}
	}
}
