package org.qdrin.qfsm.tasks;

import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FsmTasks {

	DataSource dataSource;

	private SchedulerClient schedulerClient;

  public FsmTasks(DataSource dataSource) {
    this.dataSource = dataSource;
    schedulerClient = SchedulerClient.Builder.create(dataSource)
      .serializer(new JacksonSerializer())
      .build();
  }

  public void createTask(TaskDef task) {
    log.debug("creating task: {}", task.getType());
    Consumer<TaskContext> func = task.getType().getTaskFunc();
    TaskContext ctx = new TaskContext(schedulerClient, task.getProductId(), task.getWakeAt().toInstant());
    func.accept(ctx);
  };
  public void deleteTask(TaskDef task) {
    log.debug("deleting task: {}", task.getType());
  };
}
