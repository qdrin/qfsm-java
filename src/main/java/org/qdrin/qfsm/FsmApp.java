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
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
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
			ProductClasses productClass = head.getIsCustom() ? ProductClasses.CUSTOM_BUNDLE : ProductClasses.BUNDLE;
			ProductBundle productBundle = new ProductBundle();
			Product product = new Product(head);
			product.setPartyRoleId(partyRoleId);
			product.setProductClass(productClass.ordinal());
			ArrayList<Product> components = new ArrayList<>();
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
				// component.setProductOrderItemId(componentItem.get().getProductOrderItemId());
				components.add(component);
				ProductRelationship pr = new ProductRelationship();
				pr.setProductId(component.getProductId());
				pr.setProductOfferingId(component.getProductOfferingId());
				pr.setRelationshipType(rel.getRelationshipType());
				pr.setProductOfferingName(component.getProductOfferingName());
				productRelations.add(pr);
				componentItem.get().setProductId(component.getProductId());
				orderItems.add(componentItem.get());
			}
			product.setProductRelationship(productRelations);
			head.setProductId(product.getProductId());
			productBundle.setDrive(product);
			productBundle.setBundle(product);
			productBundle.setComponents(components);
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
			ProductClasses productClass = headRelation.isPresent() ? ProductClasses.CUSTOM_BUNDLE_COMPONENT : ProductClasses.SIMPLE;
			product.setProductClass(productClass.ordinal());
			orderItem.setProductId(product.getProductId());
			orderItems.add(orderItem);
			productBundle.setBundle(product);
			productBundle.setDrive(product);
			productBundle.setComponents(null);
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
				String errString = String.format("Event contains productId: %s, that cannot be found");
				log.error( errString);
				throw new BadUserDataException(errString);
			}
			Product product = dbProduct.get();
			product.updateUserData(orderItem);
			products.add(product);
		}
		ArrayList<Product> processedProducts = new ArrayList<>();  // нужен для фильтрации уже отработанных
		List<Product> heads = products.stream()
				.filter(p -> ProductClasses.getBundles().contains(p.getProductClass()))
				.collect(Collectors.toList());
		for(Product head: heads) {
			ProductBundle bundle = new ProductBundle();
			bundle.setDrive(head);
			bundle.setBundle(head);

			List<Product> components = new ArrayList<>();
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
			bundle.setComponents(components);
			bundles.add(bundle);
			processedProducts.add(head);
		}
		for(Product product: products) {
			int index = product.getProductClass();
			ProductClasses productClass = ProductClasses.values()[index];
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
					bundle.setComponents(null);
					bundles.add(bundle);
					break;
				default:
					errString = String.format("Unexpected bundle or custom bundle remains after their processing: %s", product.getProductId());
					log.error(errString);
					throw new RuntimeException(errString);
			}
		}
		FsmResult result = new FsmResult();
		result.setBundles(bundles);
		return result;
	}

	public FsmResult sendEvent(Event event) {
		checkEvent(event);
		validateEvent(event);
		FsmResult result = event.getEventType().equals("activation_started") ? createBundles(event) : getBundles(event);
		List<ProductBundle> bundles = result.getBundles();
		if(bundles.size() != 1) {
			String errString = String.format("incorrect number of products: %d", bundles.size());
			log.error(errString);
			throw new BadUserDataException(errString);
		}
		for(ProductBundle bundle: bundles) {
			Product product = bundle.getDrive();
			String machineId = product.getProductId();
			StateMachine<String, String> machine = stateMachineService.acquireStateMachine(machineId);
			String eventType = event.getEventType();
			log.debug("machine acquired: {}", machine.getId());
			String machineState = getMachineState(machine.getState());
			var variables = machine.getExtendedState().getVariables();
			variables.put("product", product);
			log.info("current state: {}, variables: {}", machineState, variables);
			sendMessage(machine, eventType);
			machineState = getMachineState(machine.getState());
			variables = machine.getExtendedState().getVariables();
			product = machine.getExtendedState().get("product", Product.class);
			variables.remove("product");
			productRepository.save(product);
			log.info("new state: {}, variables: {}", machineState, variables);
			stateMachineService.releaseStateMachine(machineId);
		}
		eventRepository.save(event);
		return result;
	}

  private void sendMessage(StateMachine<String, String> machine, String eventName) {
		Map<Object, Object> variables = machine.getExtendedState().getVariables();

		Message<String> message = MessageBuilder
			.withPayload(eventName)
			.setHeader("origin", "user")
			.build();
		Mono<Message<String>> monomsg = Mono.just(message);
		log.debug("[{}] sending event: {}, message: {}", machine.getId(), eventName, message);
		StateMachineEventResult<String, String> res;
    try {
		  res = machine.sendEvent(monomsg).blockLast();
			log.debug("event result: {}", res);
    } catch(IllegalArgumentException e) {
			e.printStackTrace();
			String emsg = String.format("[%s] Event %s not accepted in current state: %s", machine.getId(), eventName, e.getLocalizedMessage());
      log.error(emsg);
			throw new EventDeniedException(emsg, e);
    }
		finally {
			stateMachineService.releaseStateMachine(machine.getId());
		}
		if(res == null || res.getResultType() == StateMachineEventResult.ResultType.DENIED) {
			throw new EventDeniedException(String.format("[%s] Event %s not accepted in current state", machine.getId(), eventName));
		}
	}
}
