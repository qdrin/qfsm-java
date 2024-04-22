package org.qdrin.qfsm.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPK {
  private String refId;
  private String sourceCode;
  private String eventType;
}
