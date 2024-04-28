package org.qdrin.qfsm.tasks;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.Product;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteTaskAction implements Action<String, String> {
  DataSource dataSource;

  private String taskName;
  public DeleteTaskAction(String taskName, DataSource dataSource) {
    this.taskName = taskName;
    this.dataSource = dataSource;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    String machineId = context.getExtendedState().get("product", Product.class).getProductId();
    
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
        .serializer(new JacksonSerializer())
        .build();
    log.debug("DeleteTaskAction cancelling taskId: {}", TaskInstanceId.of(taskName, machineId));
    try {
      schedulerClient.cancel(TaskInstanceId.of(taskName, machineId));
    } catch(TaskInstanceNotFoundException e) {
      log.warn("productId: {}, not found task: {}, continue", machineId, taskName);
    }
  }
}
