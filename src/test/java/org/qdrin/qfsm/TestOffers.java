package org.qdrin.qfsm;

import java.util.List;
import java.util.Map;

import org.qdrin.qfsm.model.FabricRef;
import org.qdrin.qfsm.model.ProductPrice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        public ProductPrice getPrice(String priceId) {
            ProductPrice price = prices.get(priceId);
            ObjectMapper mapper = new ObjectMapper();
            try {
                String tmp = mapper.writeValueAsString(price);
                ProductPrice res = mapper.readValue(tmp, ProductPrice.class);
                res.setId(priceId);
                return res;
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
    }
    Map<String, OfferDef> offers;
    
}
