package org.qdrin.qfsm.model.dto;

import java.time.OffsetDateTime;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventDto {
  String sourceCode;
  String refId;
  String refIdType;
  String eventType;
  @Default
  OffsetDateTime eventDate = OffsetDateTime.now();
}
