package org.qdrin.qfsm;

import org.hamcrest.TypeSafeMatcher;
import org.qdrin.qfsm.BundleBuilder.TestBundle;
import org.qdrin.qfsm.model.Product;

import lombok.extern.slf4j.Slf4j;

import static org.qdrin.qfsm.Helper.Assertions.assertProductEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

@Slf4j
public class TestBundleEquals extends TypeSafeMatcher<Map<? extends Object,?>> {
  TestBundle expected;

  TestBundleEquals(TestBundle expected) {
    // this.expected = new BundleBuilder(expected).machineState(null).build();
    this.expected = new BundleBuilder(expected).build();
  }

  private BundleBuilder BundleBuilder(TestBundle expected2) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'BundleBuilder'");
  }

  @Override
  public boolean matchesSafely(Map<? extends Object,?> variables) {
    Object o = variables.get("product");
    Object oc = variables.get("components");
    if(o == null) return false;
    if(o.getClass() != Product.class) return false;
    Product actualProduct = (Product) o;
    try {
      assertProductEquals(expected.drive, actualProduct);
    } catch(Exception e) {
      return false;
    }
    if(oc == null) return false;
    List<Product> components = (List<Product>) oc;
    try {
      assertProductEquals(expected.components(), components);
    } catch(Exception e) {
      return false;
    }
    return true;
  }

  public void describeTo(Description description) { 
    description.appendText("Drive and components equals"); 
  }

  public static Matcher<Map<? extends Object,?>> testBundleEqualTo(TestBundle expected) { 
    return new TestBundleEquals(expected); 
  }
}
