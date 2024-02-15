package org.qdrin.qfsm.repository;

import org.springframework.data.repository.CrudRepository;
import org.qdrin.qfsm.model.entity.StateMachineEntity;

public interface StateMachineCrudRepository extends CrudRepository<StateMachineEntity, String> {}
