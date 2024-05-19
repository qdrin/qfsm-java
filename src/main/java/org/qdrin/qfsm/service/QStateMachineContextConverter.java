package org.qdrin.qfsm.service;

import org.springframework.statemachine.StateMachineContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.qdrin.qfsm.model.Product;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;
import org.springframework.statemachine.region.Region;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.state.RegionState;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QStateMachineContextConverter {
  private static final int maxBufSize = 1024*1024*2048;
  public enum ProvisionOrder {
    Usage,
    Payment,
    Price
  }

  public static void recalcMachineStates(StateContext<String, String> context) {
    State<String, String> state = context.getStateMachine().getState();
    ExtendedState extendedState = context.getExtendedState();
    JsonNode machineState = toJsonNode(state);
    JsonNode componentMachineState = buildComponentMachineState(machineState);
    Product product = extendedState.get("product", Product.class);
    log.debug("machineState: {}, componentMachineState: {}", machineState, componentMachineState);
    List<Product> components = extendedState.get("components", List.class);
    product.getMachineContext().setMachineState(machineState);
    components.stream()
      .filter(c -> ! c.getMachineContext().getIsIndependent())
      .forEach(c -> c.getMachineContext().setMachineState(componentMachineState));
  }
  
  public static StateMachineContext<String, String> toContext(byte[] buffer) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    Input input = new Input(buffer);
    StateMachineContext<String, String> context = (StateMachineContext<String, String>) kryo.readClassAndObject(input);
    input.close();
    return context;
  }

  public static StateMachineContext<String, String> toContext(JsonNode jstate) {
    StateMachineContext<String, String> context = null;
    ExtendedState estate = new DefaultExtendedState();
    String state = null;
    switch(jstate.getNodeType()) {
      case OBJECT:
        Entry<String, JsonNode> entry = jstate.fields().next();
        state = entry.getKey();
        JsonNode jchild = entry.getValue();
        context = new DefaultStateMachineContext<>(state, null, null, estate);
        if(jchild.getNodeType() == JsonNodeType.ARRAY) {
          for(JsonNode j: jchild) {
            Entry<String, JsonNode> regionEntry = ((ObjectNode) j).fields().next();
            StateMachineContext<String, String> child = toContext(regionEntry.getValue());
            context.getChilds().add(child);
          }
        } else {
          StateMachineContext<String, String> child = toContext(jchild);
          context.getChilds().add(child);
        }
        break;  // ?
      case STRING:
        state = jstate.asText();
        context = new DefaultStateMachineContext<>(state, null, null, estate);
      default:
    }
    return context;
  }

  public static JsonNode toJsonNode(State<String, String> state) {
    ObjectMapper mapper = new ObjectMapper();
    if(state == null) return null;  // mapper.getNodeFactory().nullNode();
    // ObjectNode jcontext = mapper.createObjectNode();
		String stateId = state.getId();
    JsonNode result = mapper.getNodeFactory().textNode(stateId);

		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState<String, String>) state;
      ObjectNode regions = mapper.createObjectNode();
      ArrayNode childs = mapper.createArrayNode();
      ObjectNode usage = mapper.createObjectNode();
      ObjectNode payment = mapper.createObjectNode();
      ObjectNode price = mapper.createObjectNode();
			for(Region<String, String> r: rstate.getRegions()) {
        Collection<State<String, String>> regionStates = r.getStates();
        List<String> stateIds = new ArrayList<>();
        regionStates.stream().forEach(s -> stateIds.add(s.getId()));
        JsonNode child = toJsonNode(r.getState());
        if(stateIds.contains("UsageFinal")) {
          usage.set("UsageRegion", child);
        }
        if(stateIds.contains("PaymentFinal")) {
          payment.set("PaymentRegion", child);
        }
        if(stateIds.contains("PriceFinal")) {
          price.set("PriceRegion", child);
        }
			}
      childs.add(usage).add(payment).add(price);
      regions.set(stateId, childs);
      result = regions;
		}
		if(state.isSubmachineState()) {
			StateMachine<String, String> submachine = ((AbstractState<String, String>) state).getSubmachine();
			State<String, String> sstate = submachine.getState();
      JsonNode child = toJsonNode(sstate);
      ObjectNode subNode = mapper.createObjectNode().set(stateId, child);
      result = subNode;
		}
    return result;
	}


  // TODO: We cannot get StateMachine context from machine or state, should we remove this method?
  public static JsonNode toJsonNode(StateMachineContext<String, String> context) {
    ObjectMapper mapper = new ObjectMapper();
    String stateId = context.getState();
    JsonNode result = mapper.getNodeFactory().textNode(stateId);

    List<StateMachineContext<String, String>> childs = context.getChilds();
    if(childs != null && ! childs.isEmpty()) {
      if(childs.size() == 1) {
        JsonNode childNode = toJsonNode(childs.get(0));
        ObjectNode subMachine = mapper.createObjectNode().set(stateId, childNode);
        result = subMachine;
      } else {
        ObjectNode regions = mapper.createObjectNode();
        ArrayNode childNodes = mapper.createArrayNode();
        for(StateMachineContext<String, String> child: childs) {
          childNodes.add(toJsonNode(child));
        }
        regions.set(stateId, childNodes);
        result = regions;
      }
    }
    log.debug("JsonNode context: {}", result);
    return result;
  }
  
  public static byte[] toBytes(StateMachineContext<String, String> context) {
    Kryo kryo = new Kryo();
    StateMachineContextSerializer<String, String> serializer = new StateMachineContextSerializer<>();
    kryo.addDefaultSerializer(StateMachineContext.class, serializer);
    Output output = new Output(maxBufSize);
    kryo.writeClassAndObject(output, context);
    return output.toBytes();
  }

  public static JsonNode buildComponentMachineState(JsonNode bundleMachineState) {
    JsonNode result;
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode provisions = mapper.createArrayNode();
    if(bundleMachineState == null || bundleMachineState.getNodeType() == JsonNodeType.NULL) {
      result = mapper.getNodeFactory().textNode("Entry");
    } else if(bundleMachineState.getNodeType() == JsonNodeType.STRING) {
      result = bundleMachineState.deepCopy();
    } else {
      ObjectNode res = mapper.createObjectNode();
      ArrayNode bundleProvisions = (ArrayNode) bundleMachineState.get("Provision");
      ObjectNode usage = mapper.createObjectNode();
      ObjectNode payment = mapper.createObjectNode();
      ObjectNode price = mapper.createObjectNode();
      for(JsonNode prov: bundleProvisions) {
        Entry<String, JsonNode> entry = prov.fields().next();
        switch(entry.getKey()) {
          case "UsageRegion":
            usage.set("UsageRegion", entry.getValue().deepCopy());
            break;
          case "PaymentRegion":
            JsonNode payval = entry.getValue();
            payval = payval != null ? mapper.getNodeFactory().textNode("PaymentFinal") : null;
            payment.set("PaymentRegion", payval);
            break;
          case "PriceRegion":
            JsonNode priceval = entry.getValue();
            priceval = priceval != null ? mapper.getNodeFactory().textNode("PriceFinal") : null;
            price.set("PriceRegion", priceval);
            break;
          default:
        }
      }
      provisions.add(usage).add(payment).add(price);
      res.set("Provision", provisions);
      result = res;
    }  
    return result;
  }
}
