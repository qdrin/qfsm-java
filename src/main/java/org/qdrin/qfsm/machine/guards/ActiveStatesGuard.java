package org.qdrin.qfsm.machine.guards;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;

import java.util.Collection;

import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    // log.info("ActivatedGuard.evaluate productStatus: {}", prstatus);
    return res;
  }
  
}

