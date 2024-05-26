package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.List;

import org.qdrin.qfsm.PriceType;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExternalData {
  public static ProductPrice requestNextPrice(Product product) {
    ObjectMapper mapper = new ObjectMapper();
    // Application.scanner.reset();
		ProductPrice price = product.getProductPrice(PriceType.RecurringCharge).get();
    try {
      ProductPrice nextPrice = mapper.readValue(mapper.writeValueAsString(price), ProductPrice.class);
      return nextPrice;
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
		return null;
  }

  public static List<ProductCharacteristic> requestProductCharacteristics() {
    ArrayList<ProductCharacteristic> characteristics = new ArrayList<>();
    // while(false) {
    //   ProductCharacteristic characteristic = new ProductCharacteristic();
    //   characteristic.setRefName("deactivationMode");
    //   characteristic.setValue("Immediate");
    //   characteristic.setValueType("string");
    //   characteristics.add(characteristic);
    // }
		return characteristics;
  }

  public static ProductCharacteristic requestEventProperties() {
    ProductCharacteristic properties = new ProductCharacteristic();
    // while(true) {
    //   ProductCharacteristic characteristic = new ProductCharacteristic();
    //   characteristic.setRefName("eventCharacteristicName");
    //   characteristic.setValue("value");
    //   characteristic.setValueType("string");
    // }
		return properties;
  }
}
