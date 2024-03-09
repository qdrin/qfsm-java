package org.qdrin.qfsm.persist;

import java.util.HashMap;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;

public class InMemoryStateMachinePersist implements StateMachinePersist<String, String, String> {
  private final HashMap<String, StateMachineContext<String, String>> contexts = new HashMap<>();
  
  @Override
  public void write(StateMachineContext<String, String> context, String contextObj) throws Exception {
    contexts.put(contextObj, context);
  }
  @Override
  public StateMachineContext<String, String> read(String contextObj) throws Exception {
    return contexts.get(contextObj);
  }
}

