package org.qdrin.qfsm.repository;

import org.qdrin.qfsm.model.TestEntity;
import org.springframework.data.repository.CrudRepository;

public interface TestRepository extends CrudRepository<TestEntity, String> {
  
}
