package org.qdrin.qfsm.tasks;

import java.time.Duration;
import java.time.Instant;

import javax.sql.DataSource;

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

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ScheduledTasks {
  public static class TaskContext {
    public SchedulerClient schedulerClient;
    public String id;
    public String event;
    public Instant wakeAt;
  }

  public static final TaskWithoutDataDescriptor price_ended_TASK = new TaskWithoutDataDescriptor("price_ended");
  public static final TaskWithoutDataDescriptor change_price_TASK = new TaskWithoutDataDescriptor("change_price");

  public static void startPriceEndedTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(price_ended_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startChangePriceTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(change_price_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  @Bean
  Task<Void> priceEndedTask() {
    return Tasks
        .oneTime(price_ended_TASK)
        .execute((instance, ctx) -> {
            log.info("price_ended_TASK run. instance: {}, ctx: {}", instance, ctx);
    });
  }

  @Bean
  Task<Void> startChangePriceTask() {
    return Tasks
        .oneTime(change_price_TASK)
        .execute((instance, ctx) -> {
            log.info("change_price_TASK run. instance: {}, ctx: {}", instance, ctx);
    });
  }
}

