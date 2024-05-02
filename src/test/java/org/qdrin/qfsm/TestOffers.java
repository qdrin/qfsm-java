package org.qdrin.qfsm;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.FabricRef;
import org.qdrin.qfsm.model.ProductPrice;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestOffers {
    @Data
    public static class OfferDef {
        String name;
        Boolean isBundle;
        Boolean isCustom;
        List<ProductClass> productClass;
        Map<String, ProductPrice> prices;
        List<FabricRef> fabricRef;
    }
    Map<String, OfferDef> offers;
    
}
