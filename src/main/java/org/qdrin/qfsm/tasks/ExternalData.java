package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.qdrin.qfsm.Application;
import org.qdrin.qfsm.model.ProductCharacteristic;
import org.qdrin.qfsm.model.ProductPrice;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
public class ExternalData {
  public static ProductPrice requestProductPrice() {
    String input;
    Application.scanner.reset();
		ProductPrice price = new ProductPrice();
		System.out.println("Input price attributes");
		System.out.print("priceId('1'):");
    input = Application.scanner.nextLine();
    input = input.length() > 0 ? input : "1";
		price.setPriceId(input);
		System.out.print("productStatus[ACTIVE/ACTIVE_TRIAL](ACTIVE):");
    input = Application.scanner.nextLine();
    input = input.length() > 0 ? input : "ACTIVE";
		price.setProductStatus(input);
		System.out.print("duration(0):");
    input = Application.scanner.nextLine();
		price.setDuration(input.length() > 0 ? Integer.valueOf(input) : 0);
    System.out.print("nextPayDate('YYYY-MM-DDTHH:mm:SS'/null):");
    input = Application.scanner.nextLine();
    if(input.length() > 0) {
      try {
        OffsetDateTime date = OffsetDateTime.parse(input);
        price.setNextPayDate(date);
      } catch(Exception e) {
        log.error("Cannot parse your value. {}", e.getMessage());
        price.setNextPayDate(OffsetDateTime.now().plusDays(30));
      }
    } else {
      price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    }
		return price;
  }

  public static List<ProductCharacteristic> requestProductCharacteristics() {
    String input;
    Application.scanner.reset();
    ArrayList<ProductCharacteristic> characteristics = new ArrayList<>();
    while(true) {
      ProductCharacteristic characteristic = new ProductCharacteristic();
      System.out.println("Input product characterisctic");
      System.out.print("name('nothing to exit'):");
      input = Application.scanner.nextLine();
      input = input.length() > 0 ? input : null;
      if(input == null) {
        break;
      }
      characteristic.setRefName(input);
      System.out.print("value:");
      input = Application.scanner.nextLine();
      input = input.length() > 0 ? input : "";
      characteristic.setValue(input);
      characteristic.setValueType("string");
      characteristics.add(characteristic);
    }
		return characteristics;
  }

  public static ProductCharacteristic requestEventProperties() {
    String input;
    Application.scanner.reset();
    var properties = new ArrayList<>();
    while(true) {
      ProductCharacteristic characteristic = new ProductCharacteristic();
      System.out.println("Input product characterisctic");
      System.out.print("name('nothing to exit'):");
      input = Application.scanner.nextLine();
      input = input.length() > 0 ? input : null;
      if(input == null) {
        break;
      }
      characteristic.setRefName(input);
      System.out.print("value:");
      input = Application.scanner.nextLine();
      input = input.length() > 0 ? input : "";
      characteristic.setValue(input);
      characteristic.setValueType("string");
      characteristics.add(characteristic);
    }
		return characteristics;
  }
}
