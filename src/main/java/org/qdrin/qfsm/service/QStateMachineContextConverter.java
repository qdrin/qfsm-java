package org.qdrin.qfsm.service;

import org.springframework.statemachine.StateMachineContext;

import java.util.Arrays;
import java.util.List;

import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.kryo.StateMachineContextSerializer;
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
        state = jstate.fieldNames().next();
        context = new DefaultStateMachineContext<>(state, null, null, estate);
        JsonNode jchild = jstate.get(state);
        if(jchild.getNodeType() == JsonNodeType.ARRAY) {
          for(var j: jchild) {
            StateMachineContext<String, String> child = toContext(j);
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
    // ObjectNode jcontext = mapper.createObjectNode();
		String stateId = state.getId();
    JsonNode result = mapper.getNodeFactory().textNode(stateId);

		if (state.isOrthogonal()) {
			RegionState<String, String> rstate = (RegionState<String, String>) state;
      ObjectNode regions = mapper.createObjectNode();
      ArrayNode childs = mapper.createArrayNode();
			for(var r: rstate.getRegions()) {
        JsonNode child = toJsonNode(r.getState());
        childs.add(child);
			}
      regions.set(stateId, childs);
      result = regions;
		}
		if(state.isSubmachineState()) {
			StateMachine<String, String> submachine = ((AbstractState<String, String>) state).getSubmachine();
			State<String, String> sstate = submachine.getState();
      JsonNode child = toJsonNode(sstate);
      ObjectNode subMachine = mapper.createObjectNode().set(stateId, child);
      result = subMachine;
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

  public static JsonNode buildMachineState(List<String> states) {
    return buildMachineState(states.toArray(new String[0]));
  }

  public static JsonNode buildMachineState(String... states) {
    JsonNode result;
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode provisions = mapper.createArrayNode();
    JsonNode usage = null;
    JsonNode payment = null;
    JsonNode price = null;
    if(states == null || states.length == 0) {
      result = mapper.getNodeFactory().textNode("Entry");
    } else if(states.length == 1) {
      result = mapper.getNodeFactory().textNode(states[0]);
    } else {
      ObjectNode res = mapper.createObjectNode();
      res.set("Provision", provisions);
      result = res;
      for(String s: states) {
        switch(s) {
          case "PendingDisconnect":
          case "Disconnection":
          case "UsageFinal":
            usage = mapper.getNodeFactory().textNode(s);
            break;
          case "PaymentStopping":
          case "PaymentStopped":
          case "PaymentFinal":
            payment = mapper.getNodeFactory().textNode(s);
            break;
          case "PriceOff":
          case "PriceFinal":
            price = mapper.getNodeFactory().textNode(s);
            break;
          case "Prolongation":
          case "Suspending":
          case "Resuming":
          case "Suspended":
            usage = mapper.createObjectNode().put("UsageOn", s);
            break;
          case "Active":
          case "ActiveTrial":
            usage = mapper.createObjectNode().set("UsageOn", mapper.createObjectNode().put("Activated", s));
            break;
          case "Paid":
          case "WaitingPayment":
          case "NotPaid":
            payment = mapper.createObjectNode().put("PaymentOn", s);
            break;
          case "PriceActive":
          case "PriceChanging":
          case "PriceChanged":
          case "PriceNotChanged":
          case "PriceWaiting":
            price = mapper.createObjectNode().put("PriceOn", s);
            break;
          default:
            result = mapper.getNodeFactory().textNode(states[0]);
        }
      }
      provisions.add(usage).add(payment).add(price);
    }  
    return result;
  }

  public static JsonNode buildComponentMachineState(JsonNode bundleMachineState) {
    JsonNode result;
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode provisions = mapper.createArrayNode();
    JsonNode usage = null;
    if(bundleMachineState == null || bundleMachineState.isEmpty()) {
      result = mapper.getNodeFactory().textNode("Entry");
    } else if(bundleMachineState.getNodeType() == JsonNodeType.STRING) {
      result = bundleMachineState.deepCopy();
    } else {
      ObjectNode res = mapper.createObjectNode();
      ArrayNode bundleProvisions = (ArrayNode) bundleMachineState.get("Provision");
      for(JsonNode prov: bundleProvisions) {
        String name = prov.getNodeType() == JsonNodeType.STRING ? prov.toString() : ((ObjectNode) prov).fieldNames().next();
        if(Arrays.asList("UsageOn", "PendingDisconnect", "Disconnection", "UsageFinal").contains(name)) {
          usage = prov.deepCopy();
          provisions.add(usage);
        }
      }
      provisions.add("PaymentFinal");
      provisions.add("PriceFinal");
      res.set("Provision", provisions);
      result = res;
    }  
    return result;
  }
}
