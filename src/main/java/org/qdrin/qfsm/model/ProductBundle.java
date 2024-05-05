package org.qdrin.qfsm.model;

import java.util.List;
import java.util.ArrayList;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductBundle {
  Product drive;              // Product feeded to state machine
  Product bundle;             // Bundle head
  @Default
  List<Product> components = new ArrayList<>();   // bundle legs
  String errorCode;
  String errorMessage;
}
