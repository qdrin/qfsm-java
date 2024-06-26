package org.qdrin.qfsm.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.Message;

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
  Boolean isIndependent = false;
  JsonNode machineState;  // 'Classic' machineState
  List<Message<String>> deferredEvents = new ArrayList<>();     // List of deferred event id's
}
