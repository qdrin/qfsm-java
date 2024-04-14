package org.qdrin.qfsm.model.dto;

import java.time.OffsetDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventDto {
  String sourceCode;
  String refId;
  String refIdType;
  String eventType;
  OffsetDateTime eventDate;
}
