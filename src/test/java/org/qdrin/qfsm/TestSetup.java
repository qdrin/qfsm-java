package org.qdrin.qfsm;

import java.util.ArrayList;
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
public class TestSetup {
    String offerId;
    String priceId;
    ProductClass productClass;
    @Default
    List<String> componentOfferIds = new ArrayList<>();
    @Default
    JsonNode machineState = null;
}
