package org.qdrin.qfsm.model.dto;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.*;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequestDto {
  @NotNull String productId;
  List<ProductPrice> productPrice;
  List<ProductRelationship> productRelationship;
  List<ProductCharacteristic> characteristic;
  List<FabricRef> fabricRef;
  Map<String, Object> metaInfo;
  List<Characteristic> label;
}
