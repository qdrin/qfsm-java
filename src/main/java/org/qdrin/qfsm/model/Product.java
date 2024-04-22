package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.springframework.statemachine.StateMachineContext;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
  String partyRoleId;
  String productOfferingId;
  String productOfferingName;
  String productOfferingVersion;
  String productSpecificationId;
  String productSpecificationVersion;
  Boolean isBundle;
  Boolean isCustom;
  String status;
  String machineState;
  int productClass;
  int tarificationPeriod;
  OffsetDateTime trialEndDate;
  OffsetDateTime activeEndDate;
  OffsetDateTime productStartDate;
  @JdbcTypeCode(SqlTypes.JSON)
  ProductPrice productPrices;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductRelationship> productRelationship;
  @JdbcTypeCode(SqlTypes.JSON)
  List<FabricRef> fabricRef;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductCharacteristic> characteristic;
  @JdbcTypeCode(SqlTypes.JSON)
  List<Characteristic> label;
  @JdbcTypeCode(SqlTypes.JSON)
  Map<String, Object> metaInfo;
  @JdbcTypeCode(SqlTypes.JSON)
  Map<String, Object> quantity;
  // Map<String, Object> extraParams;
  public Product(ProductActivateRequestDto orderItem) {
    this.productId = UUID.randomUUID().toString();
    this.productOfferingId = orderItem.getProductOfferingId();
    this.isBundle = orderItem.getIsBundle();
    this.isCustom = orderItem.getIsCustom();
  }

  public void updateUserData(ProductRequestDto orderItem) {
    List<ProductCharacteristic> characteristic = orderItem.getCharacteristic();
    if(characteristic != null && ! characteristic.isEmpty()) {
      setCharacteristic(characteristic);
    }
    List<FabricRef> fabricRefs = orderItem.getFabricRef();
    if(fabricRefs != null && ! fabricRefs.isEmpty()) {
      setFabricRef(fabricRefs);
    }
    List<Characteristic> label = orderItem.getLabel();
    if(label != null && ! label.isEmpty()) {
      setLabel(label);
    }
    Map<String, Object> metaInfo = orderItem.getMetaInfo();
    if(metaInfo != null && ! metaInfo.isEmpty()) {
      setMetaInfo(metaInfo);
    }
  }
}
