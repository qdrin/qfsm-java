package org.qdrin.qfsm;

import java.time.OffsetDateTime;
import java.util.List;

import org.qdrin.qfsm.tasks.ActionSuite;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestExpected {
    @Default
    String status = null;
    @Default
    int tarificationPeriod = 0;
    @Default 
    int pricePeriod = 0;
    @Default
    OffsetDateTime nextPayDate = null;
    @Default
    List<String> states = null;
    @Default
    List<ActionSuite> actions = null;
    @Default
    List<ActionSuite> deleteActions = null;
    @Default
    JsonNode machineState = null;
}
