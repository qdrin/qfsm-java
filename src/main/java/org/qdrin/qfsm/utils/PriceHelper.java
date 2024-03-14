package org.qdrin.qfsm.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.statemachine.StateContext;

import lombok.extern.slf4j.Slf4j;

import org.qdrin.qfsm.model.*;

@Slf4j
public class PriceHelper {
  public static ProductPrice getProductPrice(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    return product.getProductPrices();
  }

  public static void setProductPrice(StateContext<String, String> context, ProductPrice price) {
    Product product = context.getExtendedState().get("product", Product.class);
    // List<ProductPrice> priceList = Arrays.asList(price);
    product.setProductPrices(price);
  }
}
