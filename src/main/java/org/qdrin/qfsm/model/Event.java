package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.dto.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
  @Id String refId;
  String sourceCode;
  String refIdType;
  String eventType;
  OffsetDateTime eventDate;
  ClientInfo clientInfo;
  List<Product> products;
  List<ProductResponseDto> productOrderItems;
  List<Characteristic> characteristics;
  EventProperties eventProperties;
}
