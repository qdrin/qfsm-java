package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.List;

public class TaskPlan {

   String defaultProductId;

   List<TaskDef> createPlan = new ArrayList<>();
   List<TaskDef> removePlan = new ArrayList<>();

   public TaskPlan(String productId) {
      defaultProductId = productId;
   }
   
   public boolean addToCreatePlan(TaskDef taskDef) {
      if(taskDef.getProductId() == null) taskDef.setProductId(defaultProductId);
      if(createPlan.contains(taskDef)) createPlan.remove(taskDef);
      if(removePlan.contains(taskDef)) removePlan.remove(taskDef);
      return createPlan.add(taskDef);
   }

   public boolean addToRemovePlan(TaskDef taskDef) {
      if(taskDef.getProductId() == null) taskDef.setProductId(defaultProductId);
      if(createPlan.contains(taskDef)) return createPlan.remove(taskDef);
      if(removePlan.contains(taskDef)) removePlan.remove(taskDef);
      return removePlan.add(taskDef);
   }

   public List<TaskDef> getCreatePlan() {
      return createPlan;
   }

   public List<TaskDef> getRemovePlan() {
      return removePlan;
   }

}
