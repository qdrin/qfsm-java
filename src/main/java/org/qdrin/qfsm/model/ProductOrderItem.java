package org.qdrin.qfsm.model;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductOrderItem {
  @Id String productOrderItemId;
  boolean isBundle;
  String productOfferingId;
  String ProductOfferingName;
  ProductPrice productPrices;
  List<ProductOrderItemRelationship> productOrderItemRelationships;
  List<FabricRef> fabricRefs;
  List<ProductCharacteristic> characteristics;
  List<Characteristic> label;
  Map<String, Object> metaInfo;
}
