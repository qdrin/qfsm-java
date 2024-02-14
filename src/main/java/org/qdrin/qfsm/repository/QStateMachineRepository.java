package org.qdrin.qfsm.repository;

import org.springframework.data.repository.CrudRepository;
import org.qdrin.qfsm.model.entity.StateMachineEntity;

public class QStateMachineRepository {

  public interface SMRuntimeRepository extends CrudRepository<StateMachineEntity, String> {}
}
