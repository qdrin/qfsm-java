package org.qdrin.qfsm.model.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import org.springframework.statemachine.data.RepositoryStateMachine;

@Entity
@Table(name = "state_machine")
@JsonIdentityInfo(generator= ObjectIdGenerators.IntSequenceGenerator.class)
public class StateMachineEntity extends RepositoryStateMachine {

  @Id private String machineId;
  private String state;

  @Lob
  private byte[] stateMachineContext;

  public String getMachineId() {
      return machineId;
  }

  public void setMachineId(String machineId) {
      this.machineId = machineId;
  }

  @Override
  public String getState() {
      return state;
  }

  public void setState(String state) {
      this.state = state;
  }

  @Override
  public byte[] getStateMachineContext() {
      return stateMachineContext;
  }

  public void setStateMachineContext(byte[] stateMachineContext) {
      this.stateMachineContext = stateMachineContext;
  }
}
