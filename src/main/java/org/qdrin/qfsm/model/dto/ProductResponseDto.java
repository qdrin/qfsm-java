package org.qdrin.qfsm.model.dto;

import java.util.List;

import org.qdrin.qfsm.model.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponseDto {
  @NonNull String productId;
  @NonNull String productOfferingId;
  @NonNull String status;
  String productOrderItemId;  // Must present in activation_started response only
  Boolean isBundle;
  Boolean isCustom;
  String productOfferingName;
  List<ProductRelationship> productRelationship;

  public ProductResponseDto(Product product) {
    this.productId = product.getProductId();
    this.productOfferingId = product.getProductOfferingId();
    this.status = product.getStatus();
    this.isBundle = product.getIsBundle();
    this.isCustom = product.getIsCustom();
    this.productOfferingName = product.getProductOfferingName();
    this.productRelationship = product.getProductRelationship();
  }
}
