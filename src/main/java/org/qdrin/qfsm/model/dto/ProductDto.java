package org.qdrin.qfsm.model.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.FabricRef;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.model.ProductRelationship;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDto {
  @NonNull String productId;
  String productOfferingId;
  String ProductOfferingName;
  String ProductOfferingVersion;
  String ProductSpecificationId;
  String ProductSpecificationVersion;
  boolean isBundle;
  String status;
  int tarificationPeriod;
  OffsetDateTime trialEndDate;
  OffsetDateTime activeEndDate;
  OffsetDateTime productStartDate;
  List<ProductPrice> productPrices;
  List<ProductRelationship> productRelationships;
  List<FabricRef> fabricRefs;
  List<ProductCharacteristic> characteristics;
  List<Characteristic> labels;
  Map<String, Object> metaInfo;
  Map<String, Object> quantity;
  // Map<String, Object> extraParams;
}
