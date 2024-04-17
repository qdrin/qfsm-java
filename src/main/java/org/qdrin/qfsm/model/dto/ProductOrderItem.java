package org.qdrin.qfsm.model.dto;

import java.util.List;

import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductOrderItem {
    String productOrderItemId;
    boolean isBundle;
    String productOfferingId;
    String productOfferingName;
    List<ProductCharacteristic> characteristic;
    List<ProductPrice> price;
    List<ProductOrderItemRelationshipDto> productOrderItemRelationship;
}
