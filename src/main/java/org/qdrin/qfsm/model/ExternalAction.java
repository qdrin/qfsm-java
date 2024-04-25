package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.Arrays;
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
public class ExternalAction {
  String name;
  String productId;
  String context;

  List<String> actions = Arrays.asList(
    "createFutureInternalEvent",
    "deleteFutureInternalEvent",  // need list of event at input
    "createFutureExternalEvent",
    "deleteFutureExternalEvent",  // need list of events at input
    "getExternalData",  // nextPrice, bundle composition
    "changeComponents",
    "addCustomBundleRelationship",
    "removeCustomBundleRelationship",
    "...",
    "N_doSomethingWithBundle"
  );
}
