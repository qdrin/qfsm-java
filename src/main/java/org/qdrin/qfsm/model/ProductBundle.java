package org.qdrin.qfsm.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBundle {
  Product drive;              // Product feeded to state machine
  Product bundle;             // Bundle head
  List<Product> components;   // bundle legs
  String errorCode;
  String errorMessage;
}
