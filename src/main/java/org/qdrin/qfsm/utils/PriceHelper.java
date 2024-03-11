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
    Product product = (Product) context.getExtendedState().getVariables().get("product");
    List<ProductPrice> priceList = product.getProductPrices();
    if(priceList == null) {return null;}
    return priceList.get(0);
  }

  public static void setProductPrice(StateContext<String, String> context, ProductPrice price) {
    Map<Object, Object> cvars = context.getExtendedState().getVariables();
    Product product = (Product) cvars.get("product");
    List<ProductPrice> priceList = Arrays.asList(price);
    product.setProductPrices(priceList);
    // context.getExtendedState().getVariables().put("product", product);
    log.info("setProductPrice result product: {}", cvars.get("product"));
  }
}
