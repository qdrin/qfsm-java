package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductPrice {
  String priceId;
  String productStatus;
  int duration;
  int period;
  OffsetDateTime nextPayDate;
}
