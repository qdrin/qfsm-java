package org.qdrin.qfsm.machine.states;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.support.AbstractStateMachine;

import com.fasterxml.jackson.databind.JsonNode;

import org.qdrin.qfsm.ProductClass;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.service.QStateMachineContextConverter;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class EntryEntry implements Action<String, String> {
  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    StateMachine<String, String> machine = context.getStateMachine();
    String machineId = machine.getId();
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    Product product = context.getExtendedState().get("product", Product.class);
    JsonNode machineState = product.getMachineContext().getMachineState();
    ProductClass pclass = ProductClass.values()[product.getProductClass()];
    if(machineState == null && pclass == ProductClass.CUSTOM_BUNDLE_COMPONENT) {
      Product bundle = context.getExtendedState().get("bundle", Product.class);
      JsonNode bundleState = bundle.getMachineContext().getMachineState();
      machineState = QStateMachineContextConverter.buildComponentMachineState(bundleState);
			StateMachineContext<String, String> newContext = QStateMachineContextConverter.toContext(machineState);
      product.getMachineContext().setIsIndependent(true);
      product.getMachineContext().setMachineState(machineState);
			machine.getStateMachineAccessor().doWithAllRegions(
				function -> function.resetStateMachineReactively(newContext).block()
			);
			((AbstractStateMachine<String, String>) machine).setId(machineId);
      return;
		}
    int tPeriod = product.getTarificationPeriod() + 1;
    log.info("[{}] setting tarificationPeriod={}", product.getProductId(), tPeriod);
    product.setTarificationPeriod(tPeriod);
  }
}
