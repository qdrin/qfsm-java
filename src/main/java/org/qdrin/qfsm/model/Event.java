package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.statemachine.StateMachineContext;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
  Event event;
  ClientInfo clientInfo;
  List<Product> products;
  List<ProductOrderItem> productOrderItems;
  List<EventCharacteristic> characteristics;
  List<EventProperties> eventProperties;
}
