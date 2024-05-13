package org.qdrin.qfsm;

import org.hamcrest.TypeSafeMatcher;
import org.qdrin.qfsm.tasks.TaskPlan;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class TaskPlanEquals extends TypeSafeMatcher<Map<? extends Object,?>> {
  TaskPlan expected;

  TaskPlanEquals(TaskPlan expected) {
    this.expected = expected;
  }

  @Override
  public boolean matchesSafely(Map<? extends Object,?> variables) {
    Object o = variables.get("tasks");
    if(o == null) return false;
    if(o.getClass() != TaskPlan.class) return false;
    TaskPlan actual = (TaskPlan) o;
    return Helper.isTasksEquals(expected, actual);
  }

  public void describeTo(Description description) { 
    description.appendText("TestPlan equals"); 
  }

  public static Matcher<Map<? extends Object,?>> taskPlanEqualTo(TaskPlan expected) { 
    return new TaskPlanEquals(expected); 
  }
}
