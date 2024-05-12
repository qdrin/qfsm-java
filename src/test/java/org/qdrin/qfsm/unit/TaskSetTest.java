package org.qdrin.qfsm.unit;

import static org.junit.Assert.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qdrin.qfsm.tasks.*;

public class TaskSetTest {
    
    @Test
    public void addDifferentTasksAndProducts() throws Exception {
        String productId = UUID.randomUUID().toString();
        String productId1 = UUID.randomUUID().toString();
        TaskSet set = new TaskSet();
        TaskDef def = TaskDef.builder().productId(productId).type(TaskType.PRICE_ENDED).build();
        assert(set.add(def));
        def = TaskDef.builder().productId(productId).type(TaskType.WAITING_PAY_ENDED).build();
        assert(set.add(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.PRICE_ENDED).build();
        assert(set.add(def));
        def = TaskDef.builder().productId(productId1).type(TaskType.WAITING_PAY_ENDED).build();
        assert(set.add(def));
        assertEquals(4, set.getTasks().size());
    }

    @Test
    public void addSameTasks() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        OffsetDateTime t1 = t0.plusSeconds(100);
        TaskSet set = new TaskSet();
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t1)
            .build();

        assert(set.add(def1));
        assert(! set.add(def2));
        List<TaskDef> tasks = set.getTasks();
        assertEquals(1, tasks.size());
        assert(tasks.get(0) == def1);
        assertEquals(t0, tasks.get(0).getWakeAt());
    }

    @Test
    public void putSameTasks() throws Exception {
        String productId = UUID.randomUUID().toString();
        OffsetDateTime t0 = OffsetDateTime.now();
        OffsetDateTime t1 = t0.plusSeconds(100);
        TaskSet set = new TaskSet();
        TaskDef def1 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t0)
            .build();
        TaskDef def2 = TaskDef.builder().productId(productId)
            .type(TaskType.PRICE_ENDED)
            .wakeAt(t1)
            .build();

        assert(set.add(def1));
        assert(set.put(def2));
        List<TaskDef> tasks = set.getTasks();
        assertEquals(1, tasks.size());
        assert(tasks.get(0) == def2);
        assertEquals(t1, tasks.get(0).getWakeAt());
    }
}
