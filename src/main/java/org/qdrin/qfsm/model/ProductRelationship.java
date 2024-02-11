package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;

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
  String relationshipType;
  String productOfferingId;
}
