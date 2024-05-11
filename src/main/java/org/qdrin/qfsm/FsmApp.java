package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.state.State;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.qdrin.qfsm.exception.*;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductOrderItemRelationshipDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.repository.EventRepository;
import org.qdrin.qfsm.repository.ProductRepository;
import org.qdrin.qfsm.service.QStateMachineContextConverter;
import org.qdrin.qfsm.service.QStateMachineService;
import org.qdrin.qfsm.tasks.*;
import org.qdrin.qfsm.utils.EventValidator;

@Slf4j
@Configuration
public class FsmApp {

	@Autowired
	QStateMachineService service;

	@Autowired
	EventRepository eventRepository;

	@Autowired
	ProductRepository productRepository;

  // get stringified full-state
  public JsonNode getMachineState(State<String, String> state) {
		JsonNode mstate = QStateMachineContextConverter.toJsonNode(state);
		return mstate;
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

	private Optional<Product> getCustomBundle(Product component) {
		List<ProductRelationship> rels = component.getProductRelationshipByRelationType("BELONGS");
		if(rels.size() != 1) {
			throw new BadUserDataException(
				String.format("Custom bundle component %s must have exactly 1 bundle product, now has %d",
					component.getProductId(), rels.size()));
		}
		Optional<Product> ohead = productRepository.findById(rels.get(0).getProductId());
		return ohead;
	}

	private FsmResult createBundles(Event event) {
		String partyRoleId = event.getClientInfo().getPartyRoleId();
		ArrayList<ProductBundle> bundles = new ArrayList<>();
		List<ProductActivateRequestDto> orderItems = new ArrayList<ProductActivateRequestDto>();
		final List<ProductActivateRequestDto> eventOrderItems = event.getProductOrderItems();
		log.debug("event: {}", event);
		if(eventOrderItems == null || eventOrderItems.isEmpty()) {
			throw new BadUserDataException("event productOrderItems is null or empty");
		}
		List<ProductActivateRequestDto> heads = eventOrderItems.stream()
				.filter(item -> item.getIsBundle())
				.collect(Collectors.toList());
		
		for(ProductActivateRequestDto head: heads) {
			ProductClass productClass = head.getIsCustom() ? ProductClass.CUSTOM_BUNDLE : ProductClass.BUNDLE;
			ProductBundle productBundle = new ProductBundle();
			Product product = new Product(head);
			product.setPartyRoleId(partyRoleId);
			product.setProductClass(productClass.ordinal());
			List<Product> components = productBundle.getComponents();
			List<ProductRelationship> productRelations = new ArrayList<>(); 
			List<ProductOrderItemRelationshipDto> itemRelations = head.getProductOrderItemRelationship();
			log.debug("searching bundle relationship for components {}", itemRelations);
			itemRelations = itemRelations == null ? new ArrayList<>() : itemRelations;
			for(ProductOrderItemRelationshipDto rel: itemRelations) {
				Optional<ProductActivateRequestDto> componentItem = eventOrderItems.stream()
						.filter(item -> item.getProductOrderItemId().equals(rel.getProductOrderItemId()))
						.findFirst();
				if(componentItem.isEmpty()) {
					throw new BadUserDataException(
						String.format("Cannot find component orderItem %s, specified in relations for %s", 
													rel.getProductOrderItemId(), head.getProductOrderItemId())
						);
				}
				Product component = new Product(componentItem.get());
				component.setPartyRoleId(partyRoleId);
				ProductClass pclass = productClass == ProductClass.BUNDLE ? ProductClass.BUNDLE_COMPONENT : ProductClass.CUSTOM_BUNDLE_COMPONENT;
				component.setProductClass(pclass.ordinal());
				components.add(component);
				ProductRelationship pr = new ProductRelationship(component);
				productRelations.add(pr);
				componentItem.get().setProductId(component.getProductId());
				orderItems.add(componentItem.get());
			}
			product.setProductRelationship(productRelations);
			head.setProductId(product.getProductId());
			productBundle.setDrive(product);
			productBundle.setBundle(product);
			orderItems.add(head);
			bundles.add(productBundle);
		}  // End of bundle processing, orderItems contain bundles now

		for(ProductActivateRequestDto orderItem: eventOrderItems) {
			if(orderItems.contains(orderItem)) {
				continue;
			}
			log.debug("productOrderItem {}", orderItem);
			ProductBundle productBundle = new ProductBundle();
			// drive.setProductOrderItemId(orderItem.getProductOrderItemId());
			Product product = new Product(orderItem);
			product.setPartyRoleId(partyRoleId);
			List<ProductOrderItemRelationshipDto> relations = orderItem.getProductOrderItemRelationship();
			Optional<ProductOrderItemRelationshipDto> headRelation = relations == null ?
				Optional.empty() :
				relations.stream().filter(r -> r.getRelationshipType().equals("BELONGS")).findFirst();
			ProductClass productClass = headRelation.isPresent() ? ProductClass.CUSTOM_BUNDLE_COMPONENT : ProductClass.SIMPLE;
			product.setProductClass(productClass.ordinal());
			orderItem.setProductId(product.getProductId());
			orderItems.add(orderItem);
			productBundle.setBundle(product);
			productBundle.setDrive(product);
			bundles.add(productBundle);
		}
		FsmResult result = new FsmResult();
		result.setBundles(bundles);
		result.setProductOrderItems(orderItems);
		log.debug("created {} bundles: {}", bundles.size(), bundles);
		return result;
	}

	private FsmResult getBundles(Event event) {
		ArrayList<ProductBundle> bundles = new ArrayList<>();
		final List<ProductRequestDto> eventOrderItems = event.getProducts();
		ArrayList<Product> products = new ArrayList<>();
		log.debug("event: {}", event);
		if(eventOrderItems == null || eventOrderItems.isEmpty()) {
			throw new BadUserDataException("event products is null or empty");
		}
		for(ProductRequestDto orderItem: eventOrderItems) {
			Optional<Product> dbProduct = productRepository.findById(orderItem.getProductId());
			if(dbProduct.isEmpty()) {
				String errString = String.format("Event contains productId: %s, that cannot be found", orderItem.getProductId());
				log.error( errString);
				throw new BadUserDataException(errString);
			}
			Product product = dbProduct.get();
			product.updateUserData(orderItem);
			products.add(product);
		}
		ArrayList<Product> processedProducts = new ArrayList<>();  // нужен для фильтрации уже отработанных
		List<Product> heads = products.stream()
				.filter(p -> ProductClass.getBundles().contains(p.getProductClass()))
				.collect(Collectors.toList());
		for(Product head: heads) {
			ProductBundle bundle = new ProductBundle();
			bundle.setDrive(head);
			bundle.setBundle(head);

			List<Product> components = bundle.getComponents();
			for(ProductRelationship relationship: head.getProductRelationship()) {
				Optional<Product> component = products.stream()  // Ищем по продуктам, заявленным в ордере
					.filter(p->p.getProductId().equals(relationship.getProductId()))
					.findFirst();
				if(component.isEmpty()) {  // Если не находим, подтягиваем по relationship из базы
					component = productRepository.findById(relationship.getProductId());
				}
				if(component.isEmpty()) {
					throw new RuntimeException(String.format("component %s not found for bundle %s",
										relationship.getProductId(), head.getProductId()));
				}
				processedProducts.add(component.get());
				components.add(component.get());
			}
			bundles.add(bundle);
			processedProducts.add(head);
		}
		for(Product product: products) {
			if(processedProducts.contains(product)) {
				continue;
			}
			int index = product.getProductClass();
			ProductClass productClass = ProductClass.values()[index];
			ProductBundle bundle = new ProductBundle();
			switch(productClass) {
				case BUNDLE_COMPONENT:
					String errString = String.format("Hard bundle component without bundle: %s", product.getProductId());
					log.error(errString);
					throw new BadUserDataException(errString);
				case SIMPLE:
					bundle.setBundle(product);
					bundle.setDrive(product);
					bundles.add(bundle);
					break;
				case CUSTOM_BUNDLE_COMPONENT:
					Optional<Product> ohead = getCustomBundle(product);
					if(ohead.isEmpty()) {
						throw new BadUserDataException(
							String.format("Custom bundle component %s has no head",
								product.getProductId()));
					}
					bundle.setDrive(product);
					bundle.setBundle(ohead.get());
					bundles.add(bundle);
					break;
				default:
					errString = String.format("Unexpected bundle or custom bundle remains after their processing: %s", product.getProductId());
					log.error(errString);
					throw new BadUserDataException(errString);
			}
		}
		FsmResult result = new FsmResult();
		result.setBundles(bundles);
		return result;
	}

	public FsmResult sendEvent(Event event) {
		checkEvent(event);
		EventValidator.validate(event);
		FsmResult result = event.getEventType().equals("activation_started") ? createBundles(event) : getBundles(event);
		List<ProductBundle> bundles = result.getBundles();
		if(bundles.size() != 1) {  // TODO: refactor for bulk operations
			String errString = String.format("incorrect number of products: %d", bundles.size());
			log.error(errString);
			throw new BadUserDataException(errString);
		}
		for(ProductBundle bundle: bundles) {
			Product product = bundle.getDrive();
			Product productBundle = bundle.getBundle();
			List<Product> components = bundle.getComponents();
			// StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
			// NEW
			StateMachine<String, String> machine = service.acquireStateMachine(product, bundle.getBundle(), components);
			ExtendedState extendedState = machine.getExtendedState();
			Map<Object, Object> variables = extendedState.getVariables();
			List<ActionSuite> actions = (List<ActionSuite>) variables.get("actions");
			List<ActionSuite> deleteActions = (List<ActionSuite>) variables.get("deleteActions");
			String eventType = event.getEventType();

			log.debug("machine acquired: {}", machine.getId());
			// First send deferred events cause they aren't saved in context
			List<Message<String>> deferredEvents = new ArrayList<>(product.getMachineContext().getDeferredEvents());
			for(Message<String> deferredEvent: deferredEvents) {
				try {
					StateMachineEventResult<String, String> res = machine.sendEvent(Mono.just(deferredEvent)).blockLast();
				} catch(Exception e) {
					log.warn("[{}] deferred event '{}' failed: {}", 
						machine.getId(), deferredEvent.getPayload(), e.getLocalizedMessage());
				}
			}
			StateMachineEventResult<String, String> res;
			try {
				res = machine.sendEvent(Mono.just(event.toMessage())).blockLast();
			} catch(IllegalArgumentException e) {
				String emsg = String.format("[%s] Event %s not accepted in current state: %s",
					machine.getId(), eventType, e.getLocalizedMessage());
				log.error(emsg);
				throw new EventDeniedException(emsg, e);
			} catch(Exception e) {
				e.printStackTrace();
				String emsg = String.format(
					"[%s] Event %s. Unknown error: %s", machine.getId(), eventType, e.getLocalizedMessage());
				throw new InternalError(emsg, e);
			} finally {
				service.releaseStateMachine(machine.getId());
			}
			switch(res.getResultType()) {
				case DENIED:
				String emsg = String.format(
					"[%s] Event %s not accepted in current state", machine.getId(), eventType);
					throw new EventDeniedException(emsg);
				case DEFERRED:
					Message<String> message = res.getMessage();
					log.info("[{}] Defer event. {}", machine.getId(), message);
					product.getMachineContext().getDeferredEvents().add(message);
					break;
				case ACCEPTED:
			}

			FsmActions fsmActions = new FsmActions();
			for(ActionSuite action: deleteActions) {
				fsmActions.deleteTask(action);
			}
			for(ActionSuite action: actions) {
				fsmActions.createTask(action);
			}
			if(productBundle != null && ! productBundle.getProductId().equals(product.getProductId())) {
				productRepository.save(productBundle);
			}
			components.stream().forEach(c -> productRepository.save(c));
			log.info("[{}] new state: {}, variables: {}", product.getProductId(), product.getMachineContext().getMachineState(), variables);
			productRepository.save(product);
		}
		eventRepository.save(event);
		return result;
	}
}
