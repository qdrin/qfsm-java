package org.qdrin.qfsm.model.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.qdrin.qfsm.model.*;

@Data
@ToString
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestActivateEventDto {
    EventDto event;
    ClientInfo clientInfo;
    List<ProductActivateRequestDto> productOrderItems;
}
