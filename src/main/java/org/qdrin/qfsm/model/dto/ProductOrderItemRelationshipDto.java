package org.qdrin.qfsm.model.dto;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductOrderItemRelationshipDto {
  String productOrderItemId;
  String productId;  // Для уже существующего продукта (relationshipType: "BELONGS")
  String relationshipType;
}
