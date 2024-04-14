package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.qdrin.qfsm.Application;
import org.qdrin.qfsm.model.Characteristic;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
public class ExternalData {
  public static ProductPrice requestProductPrice() {
    String input;
    // Application.scanner.reset();
		ProductPrice price = new ProductPrice();
		price.setPriceId("1");
    input = "ACTIVE";
		price.setProductStatus("ACTIVE");
		price.setDuration(0);
    System.out.print("nextPayDate('YYYY-MM-DDTHH:mm:SS'/null):");
    price.setNextPayDate(OffsetDateTime.now().plusDays(30));
		return price;
  }

  public static List<ProductCharacteristic> requestProductCharacteristics() {
    String input;
    ArrayList<ProductCharacteristic> characteristics = new ArrayList<>();
    while(true) {
      ProductCharacteristic characteristic = new ProductCharacteristic();
      input = null;
      if(input == null) {
        break;
      }
      characteristic.setRefName("deactivationMode");
      characteristic.setValue("Immediate");
      characteristic.setValueType("string");
      characteristics.add(characteristic);
    }
		return characteristics;
  }

  public static ProductCharacteristic requestEventProperties() {
    String input;
    ProductCharacteristic properties = new ProductCharacteristic();
    while(true) {
      ProductCharacteristic characteristic = new ProductCharacteristic();
      input = null;
      if(input == null) {
        break;
      }
      characteristic.setRefName("eventCharacteristicName");
      characteristic.setValue("value");
      characteristic.setValueType("string");
    }
		return properties;
  }
}
