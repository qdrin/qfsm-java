package org.qdrin.qfsm.unit;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.tasks.*;

public class TaskPlanTest {
    
    @Test
    public void addDifferentTasksToCreatePlan() throws Exception {
        String productId = UUID.randomUUID().toString();
        String productId1 = UUID.randomUUID().toString();
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def = TaskDef.builder().productId(productId).type(TaskType.PRICE_ENDED).build();
        assert(plan.addToCreatePlan(def));
        def = TaskDef.builder().productId(productId).type(TaskType.WAITING_PAY_ENDED).build();
        assert(plan.addToCreatePlan(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.PRICE_ENDED).build();
        assert(plan.addToCreatePlan(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.WAITING_PAY_ENDED).build();
        assert(plan.addToCreatePlan(def));
        assertEquals(4, plan.getCreatePlan().size());
        assertEquals(0, plan.getRemovePlan().size());
    }

    @Test
    public void addDifferentTasksToRemovePlan() throws Exception {
        String productId = UUID.randomUUID().toString();
        String productId1 = UUID.randomUUID().toString();
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def = TaskDef.builder().productId(productId).type(TaskType.PRICE_ENDED).build();
        assert(plan.addToRemovePlan(def));
        def = TaskDef.builder().productId(productId).type(TaskType.WAITING_PAY_ENDED).build();
        assert(plan.addToRemovePlan(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.PRICE_ENDED).build();
        assert(plan.addToRemovePlan(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.WAITING_PAY_ENDED).build();
        assert(plan.addToRemovePlan(def));
        assertEquals(0, plan.getCreatePlan().size());
        assertEquals(4, plan.getRemovePlan().size());
    }

    @Test
    public void addSameTasksToCreatePlan() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        OffsetDateTime t1 = t0.plusSeconds(100);
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t1)
            .build();

        assert(def1.equals(def2));
        assert(plan.addToCreatePlan(def1));
        assert(plan.addToCreatePlan(def2));
        List<TaskDef> tasks = plan.getCreatePlan();
        assertEquals(1, tasks.size());
        assert(tasks.get(0) == def2);
        assertEquals(t1, tasks.get(0).getWakeAt());
    }

    @Test
    public void addSameTasksToRemovePlan() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        OffsetDateTime t1 = t0.plusSeconds(100);
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t1)
            .build();

        assert(plan.addToRemovePlan(def1));
        assert(plan.addToRemovePlan(def2));
        List<TaskDef> tasks = plan.getRemovePlan();
        assertEquals(1, tasks.size());
        assert(tasks.get(0) == def2);
        assertEquals(t1, tasks.get(0).getWakeAt());
    }

    @Test
    public void addRemoveToExistingCreate() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .build();

        List<TaskDef> tasks = plan.getCreatePlan();
        List<TaskDef> deleteTasks = plan.getRemovePlan();
        assert(plan.addToCreatePlan(def1));
        assertEquals(1, tasks.size());
        assertEquals(0, deleteTasks.size());
        assert(plan.addToRemovePlan(def2));
        assertEquals(0, tasks.size());
        assertEquals(0, deleteTasks.size());
    }

    @Test
    public void addCreateToExistingRemove() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        TaskPlan plan = new TaskPlan(productId);
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .build();

        List<TaskDef> tasks = plan.getCreatePlan();
        List<TaskDef> deleteTasks = plan.getRemovePlan();
        assert(plan.addToRemovePlan(def2));
        assertEquals(0, tasks.size());
        assertEquals(1, deleteTasks.size());
        assert(plan.addToCreatePlan(def1));
        assertEquals(1, tasks.size());
        assertEquals(0, deleteTasks.size());
    }
}
