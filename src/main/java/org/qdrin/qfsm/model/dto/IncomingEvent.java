package org.qdrin.qfsm.model.dto;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IncomingEvent {
  BaseEvent event;
  ClientInfo clientInfo;
  List<Product> products;
  List<ProductOrderItem> productOrderItems;
  List<Characteristic> characteristics;
  EventProperties eventProperties;
}
