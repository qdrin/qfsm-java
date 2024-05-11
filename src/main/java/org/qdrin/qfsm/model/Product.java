package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;
import org.qdrin.qfsm.model.dto.ProductRequestDto;

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
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  MachineContext machineContext = new MachineContext();
  @Builder.Default
  int productClass = 0;
  int tarificationPeriod;
  OffsetDateTime trialEndDate;
  OffsetDateTime activeEndDate;
  OffsetDateTime productStartDate;
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  List<ProductPrice> productPrice = new ArrayList<>();
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  List<ProductRelationship> productRelationship = new ArrayList<>();
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  List<FabricRef> fabricRef = new ArrayList<>();
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  List<ProductCharacteristic> characteristic = new ArrayList<>();
  
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  List<Characteristic> label = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  Map<String, Object> metaInfo = new HashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  Map<String, Object> quantity = new HashMap<>();
  
  // Map<String, Object> extraParams;
  public Product(ProductActivateRequestDto orderItem) {
    this.productId = UUID.randomUUID().toString();
    this.productOfferingId = orderItem.getProductOfferingId();
    this.productOfferingName = orderItem.getProductOfferingName();
    this.isBundle = orderItem.getIsBundle();
    this.isCustom = orderItem.getIsCustom();
    this.machineContext = new MachineContext();
    if(orderItem.getCharacteristic() != null) { this.characteristic = orderItem.getCharacteristic(); }
    if(orderItem.getMetaInfo() != null) { this.metaInfo = orderItem.getMetaInfo(); }
    if(orderItem.getLabel() != null) { this.label = orderItem.getLabel(); }
    if(orderItem.getFabricRef() != null) { this.fabricRef = orderItem.getFabricRef(); }
    if(orderItem.getProductPrice() != null) { this.productPrice = orderItem.getProductPrice(); }
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

  public Optional<ProductPrice> getProductPrice(PriceType priceType) {
    return productPrice.stream()
      .filter(p -> p.getPriceType().equals(priceType.name()))
      .findFirst();
      // .collect(Collectors.toList());
  }

  public final List<ProductRelationship> getProductRelationshipByRelationType(String relationType) {
    return this.productRelationship
        .stream()
        .filter(r -> r.getRelationshipType().equals(relationType))
        .collect(Collectors.toList());
  }
}
