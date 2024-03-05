package org.qdrin.qfsm.repository;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

import org.qdrin.qfsm.model.entity.ProductEntity;

public interface ProductRepository extends CrudRepository<ProductEntity, String> {

  // List<ProductEntity> findByPartyRoleIdAndProductOfferingId(String partyRoleId, String productOfferingId);

  // ProductEntity findByProductId(String productId);
}
