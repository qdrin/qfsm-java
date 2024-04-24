package org.qdrin.qfsm.utils;

import org.springframework.statemachine.StateContext;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import org.qdrin.qfsm.model.*;

@Slf4j
public class PriceHelper {
  public static ProductPrice getProductPrice(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    return product.getProductPrice().get(0);
  }

  public static void setProductPrice(StateContext<String, String> context, ProductPrice price) {
    Product product = context.getExtendedState().get("product", Product.class);
    // List<ProductPrice> priceList = Arrays.asList(price);
    product.setProductPrice(Arrays.asList(price));
  }
}
