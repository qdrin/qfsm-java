package org.qdrin.qfsm.model.dto;

import java.util.List;

import org.qdrin.qfsm.model.*;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponseDto {
  @NotNull String productId;
  @NotNull String productOfferingId;
  @NotNull String status;
  String productOrderItemId;  // Must present in activation_started response only
  Boolean isBundle;
  Boolean isCustom;
  String ProductOfferingName;
  List<ProductRelationship> productRelationship;
}
