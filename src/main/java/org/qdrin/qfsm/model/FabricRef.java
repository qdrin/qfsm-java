package org.qdrin.qfsm.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FabricRef {
  String fabricId;
  String fabricProductId;
  String fabricProductOfferingId;
}
