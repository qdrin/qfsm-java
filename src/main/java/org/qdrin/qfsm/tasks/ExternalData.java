package org.qdrin.qfsm.tasks;

import java.util.Scanner;

import org.qdrin.qfsm.model.ProductPrice;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
public class ExternalData {
  public static ProductPrice RequestProductPrice() {
    Scanner in = new Scanner(System.in);
    String input;
		ProductPrice price = new ProductPrice();
		System.out.println("Input price attributes");
		System.out.print("priceId('1'):");
    input = in.nextLine();
    input = input.length() > 0 ? input : "1";
		price.setPriceId(input);
		System.out.print("productStatus[ACTIVE/ACTIVE_TRIAL](ACTIVE):");
    input = in.nextLine();
    input = input.length() > 0 ? input : "ACTIVE";
		price.setProductStatus(input);
		System.out.print("duration(0):");
    input = in.nextLine();
		price.setDuration(input.length() > 0 ? Integer.valueOf(input) : 0);
    System.out.print("nextPayDate('YYYY-MM-DDTHH:mm:SS'/null):");
    input = in.nextLine();
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
    // in.close();
		return price;
  }
}
