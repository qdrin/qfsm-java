package org.qdrin.qfsm.persist;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.qdrin.qfsm.model.ContextEntity;
import org.qdrin.qfsm.repository.ContextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

@Configuration
public class ProductStateMachinePersist implements StateMachinePersist<String, String, String> {
  Map<String, StateMachineContext<String, String>> contexts = new HashMap<>();

  final static int bufferSize = 1024*1024*1024;

  @Autowired
  private ContextRepository contextRepository;
  
  private static QStateMachineContextConverter converter = new QStateMachineContextConverter();

  private void clearVariables(StateMachineContext<String, String> context, boolean clearSelf) {
    if(clearSelf) {
      context.getExtendedState().getVariables().clear();
    }
    for(StateMachineContext<String, String> child: context.getChilds()) {
      clearVariables(child, true);
    }
  }

  // @Override
  public void write(StateMachineContext<String, String> context, String machineId) throws Exception {
    clearVariables(context, false);
    ContextEntity ce = new ContextEntity();
    ce.setProductId(machineId);
    ce.setContext(converter.toBytes(context));
    contextRepository.save(ce);
  }
  
  // @Override
  public StateMachineContext<String, String> read(String machineId) throws Exception {
    Optional<ContextEntity> cep = contextRepository.findById(machineId);
    if(cep.isEmpty()) {
      return null;
    }
    byte[] bcontext = cep.get().getContext();
    StateMachineContext<String, String> context = converter.toContext(bcontext);
    return context;
  }
}

