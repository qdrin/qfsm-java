package org.qdrin.qfsm.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TaskSet {

   HashSet<TaskDef> tasks = new HashSet<>();
   
   public boolean add(TaskDef taskDef) {
      return tasks.add(taskDef);
   }

   public boolean remove(TaskDef taskDef) {
      return tasks.remove(taskDef);
   }

   public boolean put(TaskDef taskDef) {
      if(tasks.contains(taskDef)) {
         tasks.remove(taskDef);
      }
      return tasks.add(taskDef);
   }

   public List<TaskDef> getTasks() {
      return new ArrayList<>(tasks);
   }

}
