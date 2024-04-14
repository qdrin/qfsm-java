package org.qdrin.qfsm.model.dto;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.FabricRef;
import org.qdrin.qfsm.model.ProductPrice;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductActivateRequestDto {
  @NotNull String productOrderItemId;  // Must present in activation_started response only
  @NotNull String productOfferingId;
  List<ProductPrice> productPrice;
  boolean isBundle;
  boolean isCustom;
  String ProductOfferingName;
  List<ProductOrderItemRelationshipDto> productOrderItemRelationship;
  List<FabricRef> fabricRef;
  Map<String, Object> metaInfo;
  List<Characteristic> label;
}
