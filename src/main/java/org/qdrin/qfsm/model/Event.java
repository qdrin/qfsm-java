package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
public class Event {
  String eventId;
  String sourceCode;
  String refId;
  String refIdType;
  String eventType;
  OffsetDateTime eventDate;
  String refItemId;
  String refItemIdType;
}
