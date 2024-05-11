package org.qdrin.qfsm.model;

import java.util.ArrayList;
import java.util.List;

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
public class MachineContext {
  JsonNode machineState;  // 'Classic' machineState
  List<String> deferredEvents = new ArrayList<>();     // List of deferred event id's
}
