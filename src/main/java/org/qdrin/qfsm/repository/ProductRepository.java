package org.qdrin.qfsm.repository;

import org.springframework.data.repository.CrudRepository;

import org.qdrin.qfsm.model.Product;

public interface ProductRepository extends CrudRepository<Product, String> {

  // List<ProductEntity> findByPartyRoleIdAndProductOfferingId(String partyRoleId, String productOfferingId);
}
