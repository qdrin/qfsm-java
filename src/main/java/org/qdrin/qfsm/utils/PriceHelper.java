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
    // cvars.put("product", product);
    log.info("setProductPrices result product: {}", cvars.get("product"));
  }

  public static ProductPrice getNextPrice(StateContext<String, String> context) {
    List<ProductPrice> priceList = (List<ProductPrice>) context.getExtendedState().getVariables().get("nextPrice");
    if(priceList == null) {return null;}
    return priceList.get(0);
  }

  public static void setNextPrice(StateContext<String, String> context, ProductPrice price) {
    var cvars = context.getExtendedState().getVariables();
    Product product = (Product) cvars.get("product");
    List<ProductPrice> priceList = Arrays.asList(price);
    cvars.put("nextPrice", priceList);
  }
}
