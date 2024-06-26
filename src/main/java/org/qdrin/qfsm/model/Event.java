package org.qdrin.qfsm.model;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.dto.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Entity
@Table(name = "events")
@IdClass(EventPK.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
  @Id String refId;
  @Id String sourceCode;
  @Id String eventType;
  String refIdType;
  OffsetDateTime eventDate;
  @JdbcTypeCode(SqlTypes.JSON)
  ClientInfo clientInfo;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductRequestDto> products;
  @JdbcTypeCode(SqlTypes.JSON)
  List<ProductActivateRequestDto> productOrderItems;
  @JdbcTypeCode(SqlTypes.JSON)
  List<Characteristic> characteristics;
  @JdbcTypeCode(SqlTypes.JSON)
  EventProperties eventProperties;

  public Event(RequestEventDto eventDto) {
    EventDto ev = eventDto.getEvent();
    this.refId = ev.getRefId();
    this.refIdType = ev.getRefIdType();
    this.sourceCode = ev.getSourceCode();
    this.eventDate = ev.getEventDate();
    this.eventType = ev.getEventType();
    this.clientInfo = eventDto.getClientInfo();
    this.products = eventDto.getProducts();
    this.productOrderItems = eventDto.getProductOrderItems();
    this.characteristics = eventDto.getCharacteristics();
    this.eventProperties = eventDto.getEventProperties();
  }

  public Message<String> toMessage() {
    return toMessage(null);
  }
  public Message<String> toMessage(List<ProductPrice> userPrice) {
      Message<String> message = MessageBuilder
        .withPayload(eventType)
        .setHeader("clientInfo", clientInfo)
        .setHeader("eventDate", eventDate)
        .setHeader("characteristics", characteristics)
        .setHeader("eventProperties", eventProperties)
        .setHeader("userPrice", userPrice)
			  .build();
      return message;
  }
}
