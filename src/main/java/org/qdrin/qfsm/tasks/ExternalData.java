package org.qdrin.qfsm.tasks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Scanner;

import org.qdrin.qfsm.model.ProductPrice;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;

@Slf4j
public class ExternalData {
  public static ProductPrice RequestProductPrice() {
    Scanner in = new Scanner(System.in);
		ProductPrice price = new ProductPrice();
		System.out.println("Input price attributes");
		System.out.print("priceId:");
		price.setPriceId(in.nextLine());
		System.out.print("productStatus[ACTIVE/ACTIVE_TRIAL]:");
		price.setProductStatus(in.nextLine());
		System.out.print("duration:");
		price.setDuration(Integer.valueOf(in.nextLine()));
    System.out.print("nextPayDate('YYYY-MM-DDTHH:mm:SS'):");
    String dateStr = in.nextLine();
    // @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)
    try {
      OffsetDateTime date = OffsetDateTime.parse(dateStr);
      price.setNextPayDate(date);
    } catch(Exception e) {
      log.error("Cannot parse your value. {}", e.getMessage());
      price.setNextPayDate(OffsetDateTime.now().plusDays(30));
    }
		return price;
  }
}
