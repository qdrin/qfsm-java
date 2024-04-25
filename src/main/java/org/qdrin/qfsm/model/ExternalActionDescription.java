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
public class ExternalActionDescription {
  String name;
  String productId;
  String context;

  List<String> actions = Arrays.asList(
    "createFutureInternalEvent",
    "deleteFutureInternalEvent",  // need list of event at input
    "createFutureExternalEvent",
    "deleteFutureExternalEvent",  // need list of events at input
    "immediateExternalEvent"      // Если будет нужен: отдельный вид action-а, запускаемый в синхроне после успешного прощелкивания FSM
    // "getExternalData",  // actions, не меняющие состояние систем, запускаем из FSM: nextPrice, bundle composition
    // "changeComponents", add/removeCustomBundleRelation   // Делаем внутри FSM, в случае ошибки откат будет не нужен
  );
}
