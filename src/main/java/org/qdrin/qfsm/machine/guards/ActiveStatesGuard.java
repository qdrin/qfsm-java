package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import java.util.Collection;

public class ActiveStatesGuard implements Guard<String, String> {
  private final Collection<String> stateIds;

  public ActiveStatesGuard(Collection<String> stateIds) {
    this.stateIds = stateIds;
  }

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    Collection<String> currentStateIds = context.getStateMachine().getState().getIds();
    boolean res = true;
    for(String id: stateIds) {
      if(! currentStateIds.contains(id)) {
        res = false;
        break;
      }
    }
    return res;
  }
  
}

