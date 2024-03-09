package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;

import lombok.AccessLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductPrice {
  String priceId;
  String productStatus;
  int duration;
  int period;
  OffsetDateTime nextPayDate;
}
