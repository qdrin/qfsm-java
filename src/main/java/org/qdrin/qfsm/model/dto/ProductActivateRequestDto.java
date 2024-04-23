package org.qdrin.qfsm.model.dto;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.FabricRef;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductActivateRequestDto {
  @NotNull String productOrderItemId;  // Must present in activation_started response only
  @NotNull String productOfferingId;
  String productId;
  List<ProductPrice> productPrice;
  @Default Boolean isBundle = false;
  @Default Boolean isCustom = false;
  String ProductOfferingName;
  List<ProductOrderItemRelationshipDto> productOrderItemRelationship;
  List<ProductCharacteristic> characteristic;
  List<FabricRef> fabricRef;
  Map<String, Object> metaInfo;
  List<Characteristic> label;
}
