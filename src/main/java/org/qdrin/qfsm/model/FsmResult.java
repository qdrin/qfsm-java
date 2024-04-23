package org.qdrin.qfsm.model;

import java.util.List;

import org.qdrin.qfsm.model.dto.ProductActivateRequestDto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FsmResult {
  List<ProductActivateRequestDto> productOrderItems;     // initial request. productId's are added for activation_started event
  List<ProductBundle> bundles;   // results
}
