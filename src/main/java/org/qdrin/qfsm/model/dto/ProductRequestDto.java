package org.qdrin.qfsm.model.dto;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequestDto {
  @NonNull String productId;
  List<ProductCharacteristic> characteristic;
  List<ProductPrice> productPrice;
  List<ProductRelationship> productRelationship;
  List<FabricRef> fabricRef;
  Map<String, Object> metaInfo;
  List<Characteristic> label;
}
