package org.qdrin.qfsm.repository;

import org.qdrin.qfsm.model.ContextEntity;
import org.qdrin.qfsm.model.Product;
import org.springframework.data.repository.CrudRepository;

public interface ContextRepository extends CrudRepository<ContextEntity, String> {
  // List<ProductEntity> findByPartyRoleIdAndProductOfferingId(String partyRoleId, String productOfferingId);
}