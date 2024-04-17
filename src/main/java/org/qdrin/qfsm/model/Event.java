package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.dto.*;

@Entity
@Table(name = "events")
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
  @JdbcTypeCode(SqlTypes.JSON)
  ClientInfo clientInfo;
  @JdbcTypeCode(SqlTypes.JSON)
  List<Product> products;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductResponseDto> productOrderItems;
  @JdbcTypeCode(SqlTypes.JSON)
  List<Characteristic> characteristics;
  @JdbcTypeCode(SqlTypes.JSON)
  EventProperties eventProperties;
}
