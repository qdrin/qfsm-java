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
  String id;
  String name;
  String priceType;
  String productStatus;
  String recurringChargePeriodType;
  int recurringChargePeriodLength;
  int duration;
  int period;
  OffsetDateTime nextPayDate;
  String tarificationTag;
  Object nextEntity;
  Object priceAlterations;
  Object tax;
  Object price;
  Object unitOfMeasure;
  Object validFor;
  Object href;
  Object psiSpecific;
  Object value;
}
