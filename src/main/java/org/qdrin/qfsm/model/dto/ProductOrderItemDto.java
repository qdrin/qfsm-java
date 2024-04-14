package org.qdrin.qfsm.model;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductOrderItemDto {
  @NonNull String productId;
  @NonNull String productOfferingId;
  @NonNull String status;
  String productOrderItemId;  // Must present in activation_started response only
  boolean isBundle;
  boolean isCustom;
  String ProductOfferingName;
  List<ProductRelationship> productRelationship;
}
