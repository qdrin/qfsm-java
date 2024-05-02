package org.qdrin.qfsm;

import java.util.Arrays;
import java.util.List;

public enum ProductClass {
  VOID,
  SIMPLE,
  BUNDLE,
  BUNDLE_COMPONENT,
  CUSTOM_BUNDLE,
  CUSTOM_BUNDLE_COMPONENT;
  static List<Integer> getBundles() {
    return Arrays.asList(BUNDLE.ordinal(), CUSTOM_BUNDLE.ordinal());
  }
}
