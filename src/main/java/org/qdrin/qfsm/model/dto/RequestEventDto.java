package org.qdrin.qfsm.model.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.*;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Client;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestEventDto {
    EventDto event;
    ClientInfo clientInfo;
    List<ProductRequestDto> products;
    List<ProductActivateRequestDto> productOrderItems;
    List<Characteristic> characteristics;
    EventProperties eventProperties;
}
