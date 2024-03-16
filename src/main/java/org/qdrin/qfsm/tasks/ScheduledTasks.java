package org.qdrin.qfsm.tasks;

import java.time.Duration;
import java.time.Instant;

import javax.sql.DataSource;

import org.qdrin.qfsm.FsmApp;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ScheduledTasks {
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TaskContext {
    public SchedulerClient schedulerClient;
    public String id;
    public Instant wakeAt;
  }

  @Autowired
  FsmApp fsmApp;

  public static final TaskWithoutDataDescriptor price_ended_TASK = new TaskWithoutDataDescriptor("price_ended");
  public static final TaskWithoutDataDescriptor change_price_TASK = new TaskWithoutDataDescriptor("change_price");
  public static final TaskWithoutDataDescriptor disconnect_TASK = new TaskWithoutDataDescriptor("disconnect");
  public static final TaskWithoutDataDescriptor waiting_pay_ended_TASK = new TaskWithoutDataDescriptor("waiting_pay_ended");

  public static void startPriceEndedTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(price_ended_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startChangePriceTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(change_price_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startDisconnectTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(disconnect_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startWaitingPayEndedTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(waiting_pay_ended_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  @Bean
  Task<Void> disconnectTask() {
    return Tasks
        .oneTime(disconnect_TASK)
        .execute(getTaskInstance(disconnect_TASK));
  }

  @Bean
  Task<Void> priceEndedTask() {
    return Tasks
        .oneTime(price_ended_TASK)
        .execute(getTaskInstance(price_ended_TASK));
  }

  @Bean
  Task<Void> changePriceTask() {
    return Tasks
        .oneTime(change_price_TASK)
        .execute(getTaskInstance(change_price_TASK));
  }

  @Bean
  Task<Void> waitingPayEndedTask() {
    return Tasks
        .oneTime(waiting_pay_ended_TASK)
        .execute(getTaskInstance(waiting_pay_ended_TASK));
  }

  private VoidExecutionHandler<Void> getTaskInstance(TaskWithoutDataDescriptor descriptor) {
    return (instance, ctx) -> {
      log.info("task instance run. productId: {}, taskName: {}", instance.getId(), instance.getTaskName());
      fsmApp.sendEvent(instance.getId(), instance.getTaskName());
    };
  }
}

