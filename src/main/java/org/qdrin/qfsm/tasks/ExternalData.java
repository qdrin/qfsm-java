package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.List;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import java.time.OffsetDateTime;

public class ExternalData {
  public static ProductPrice requestProductPrice() {
    // Application.scanner.reset();
		ProductPrice price = new ProductPrice();
		price.setId("1");
		price.setProductStatus("ACTIVE");
		price.setDuration(0);
    System.out.print("nextPayDate('YYYY-MM-DDTHH:mm:SS'/null):");
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
		return price;
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
