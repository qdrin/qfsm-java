package org.qdrin.qfsm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.EventProperties;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;

public class DisconnectModeCalculator {
    // TODO: Check names and values in analytics
    private static final String eventCharName = "deactivation_mode";
    private static final String eventCharImmediateType = "Immediate";
    private static final String eventCharPostponedType = "Postponed";

    private static final String productCharName = "deactivation_mode";
    private static final String productCharImmediateType = "Immediate";
    private static final String productCharPostponedType = "Postponed";
    
    public enum DisconnectMode {
        POSTPONED,
        IMMEDIATE;
    }

    public static DisconnectMode calculate(Product product, List<Characteristic> eventChars, EventProperties eventProperties) {        

        DisconnectMode mode = DisconnectMode.POSTPONED;
        if(product != null) {
            List<ProductCharacteristic> productChars = new ArrayList<>(product.getCharacteristic());
            Optional<ProductCharacteristic> productChar = productChars.stream()
                .filter(c -> {return c.getRefName().equals(productCharName);})
                .findFirst();
            String productMode = productChar.isPresent() ? productChar.get().getValue().toString() : productCharPostponedType;
            switch(productMode) {
                case productCharImmediateType:
                    mode = DisconnectMode.IMMEDIATE;
                    break;
                default:
                    mode = DisconnectMode.POSTPONED;
            }
        }

        if(eventChars != null) {
            Optional<Characteristic> omode = eventChars.stream().filter(c -> c.getName().equals(eventCharName)).findFirst();
            String eventMode = omode.isPresent() ? omode.get().getValue().toString() : eventCharPostponedType;
            switch(eventMode) {
                case eventCharImmediateType:
                    mode = DisconnectMode.IMMEDIATE;
                    break;
                case eventCharPostponedType:
                    mode = DisconnectMode.POSTPONED;
                    break;
                default:
            }
        }
        return mode;
    } 
}
