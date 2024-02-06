package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
  String productId;
  String PartyRoleId;
  String productOfferingId;
  String ProductOfferingName;
  String ProductOfferingVersion;
  String ProductSpecificationId;
  String ProductSpecificationVersion;
  boolean isBundle;
  String status;
  int productClass;
  List<ProductPrice> productPrices;
  OffsetDateTime trialEndDate;
  OffsetDateTime activeEndDate;
  OffsetDateTime productStartDate;
  int tarificationPeriod;
  List<ProductRelationship> productRelationships;
  List<FabricRef> fabricRefs;
  List<ProductCharacteristic> characteristics;
  List<Label> labels;
  Map<String, Object> metaInfo;
  Map<String, Object> quantity;
  Map<String, Object> extraParams;
}
