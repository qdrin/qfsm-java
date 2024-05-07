package org.qdrin.qfsm.model;

import org.qdrin.qfsm.ProductClass;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRelationship {
  String productId;
  String productOfferingId;
  String relationshipType;
  String productOfferingName;

  public ProductRelationship(Product toProduct) {
    this.productId = toProduct.getProductId();
    this.productOfferingId = toProduct.getProductOfferingId();
    this.productOfferingName = toProduct.getProductOfferingName();
    ProductClass pclass = ProductClass.values()[toProduct.getProductClass()];
    String relationType;
    switch(pclass) {
      case CUSTOM_BUNDLE_COMPONENT:
        relationType = "CUSTOM_BUNDLES"; break;
      case BUNDLE_COMPONENT:
        relationType = "BUNDLES"; break;
      case CUSTOM_BUNDLE:
        relationType = "BELONGS"; break;
      default:
        relationType = null;
    }
    this.relationshipType = relationType;
  }
}
