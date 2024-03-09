package org.qdrin.qfsm.repository;

import org.qdrin.qfsm.model.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, String> {
  // List<ProductEntity> findByPartyRoleIdAndProductOfferingId(String partyRoleId, String productOfferingId);
}