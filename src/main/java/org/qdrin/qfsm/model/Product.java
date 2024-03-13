package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.statemachine.StateMachineContext;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
  @Id String productId;
  String PartyRoleId;
  String productOfferingId;
  String ProductOfferingName;
  String ProductOfferingVersion;
  String ProductSpecificationId;
  String ProductSpecificationVersion;
  boolean isBundle;
  String status;
  int productClass;
  int tarificationPeriod;
  OffsetDateTime trialEndDate;
  OffsetDateTime activeEndDate;
  OffsetDateTime productStartDate;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductPrice> productPrices;
  // List<ProductRelationship> productRelationships;
  // List<FabricRef> fabricRefs;
  // List<ProductCharacteristic> characteristics;
  // List<Label> labels;
  // Map<String, Object> metaInfo;
  // Map<String, Object> quantity;
  // Map<String, Object> extraParams;
  @Lob
  byte[] context;
  public Product(String productId) {
    this.productId = productId;
  }
}
