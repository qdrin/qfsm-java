package org.qdrin.qfsm.tasks;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.qdrin.qfsm.FsmApp;
import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.dto.ProductRequestDto;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

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
  ProductRepository productRepository;

  @Autowired
  FsmApp fsmApp;

  Event.EventBuilder internalEventBuilder(TaskInstance<Void> instance, ExecutionContext ctx) {
    String productId = instance.getId();
    String eventType = instance.getTaskName();

    Product product = productRepository.findById(productId).get();
    ProductRequestDto productRequest = ProductRequestDto.builder()
      .productId(productId)
      .build();
    Event.EventBuilder builder = Event.builder()
      .refId(instance.getTaskAndInstance())
      .refIdType("taskEvent")
      .sourceCode("PSI")
      .eventType(eventType)
      .products(Arrays.asList(productRequest));
    return builder;
  }

  public static final TaskWithoutDataDescriptor abort_TASK = new TaskWithoutDataDescriptor("abort");
  public static final TaskWithoutDataDescriptor price_ended_TASK = new TaskWithoutDataDescriptor("price_ended");
  public static final TaskWithoutDataDescriptor change_price_TASK = new TaskWithoutDataDescriptor("change_price");
  public static final TaskWithoutDataDescriptor disconnect_TASK = new TaskWithoutDataDescriptor("disconnect");
  public static final TaskWithoutDataDescriptor waiting_pay_ended_TASK = new TaskWithoutDataDescriptor("waiting_pay_ended");
  public static final TaskWithoutDataDescriptor suspend_ended_TASK = new TaskWithoutDataDescriptor("suspend_ended");
  public static final TaskWithoutDataDescriptor prolong_external_TASK = new TaskWithoutDataDescriptor("PROLONG");
  public static final TaskWithoutDataDescriptor suspend_external_TASK = new TaskWithoutDataDescriptor("SUSPEND");
  public static final TaskWithoutDataDescriptor resume_external_TASK = new TaskWithoutDataDescriptor("RESUME");
  public static final TaskWithoutDataDescriptor disconnect_external_TASK = new TaskWithoutDataDescriptor("DISCONNECT");
  public static final TaskWithoutDataDescriptor change_price_external_TASK = new TaskWithoutDataDescriptor("CHANGE_PRICE");
  public static final TaskWithoutDataDescriptor disconnect_external_external_TASK = new TaskWithoutDataDescriptor("DISCONNECT_EXTERNAL");

  public static void startAbortTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(abort_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

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

  public static void startSuspendEndedTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(suspend_ended_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startProlongExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(prolong_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startSuspendExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(suspend_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startResumeExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(resume_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startDisconnectExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(disconnect_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startChangePriceExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(change_price_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  public static void startDisconnectExternalExternalTask(TaskContext taskContext) {
    taskContext.schedulerClient.schedule(disconnect_external_external_TASK.instance(taskContext.id), taskContext.wakeAt);
  }

  @Bean
  Task<Void> abortTask() {
    return Tasks
        .oneTime(abort_TASK)
        .execute(getTaskInstance(abort_TASK));
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
        .execute((instance, ctx) -> {
          String productId = instance.getId();
          String eventType = instance.getTaskName();
          log.info("task instance run. productId: {}, taskName: {}", productId, eventType);
          Product product = productRepository.findById(productId).get();
          if(product == null) {
            return;
          }
          // TODO: Change plannedPrice to price-calc request
          ProductPrice nextPrice = ExternalData.requestNextPrice(product);
          Characteristic nextPriceChar = Characteristic.builder()
            .valueType(ProductPrice.class.getSimpleName())
            .value(nextPrice)
            .name("nextPrice")
            .build();
          Event event = internalEventBuilder(instance, ctx)
            .characteristics(Arrays.asList(nextPriceChar))
            .build();
          /////////////////////////////////////////////////////
          
          //  fsmApp.setVariable(productId, "nextPrice", nextPrice);
          // fsmApp.sendEvent(instance.getId(), "change_price");
          });
  }

  @Bean
  Task<Void> waitingPayEndedTask() {
    return Tasks
        .oneTime(waiting_pay_ended_TASK)
        .execute(getTaskInstance(waiting_pay_ended_TASK));
  }

  @Bean
  Task<Void> suspendEndedTask() {
    return Tasks
        .oneTime(suspend_ended_TASK)
        .execute(getTaskInstance(suspend_ended_TASK));
  }

  @Bean
  Task<Void> prolongExternalTask() {
    return Tasks
        .oneTime(prolong_external_TASK)
        .execute(getExternalTaskInstance(prolong_external_TASK));
  }

  @Bean
  Task<Void> suspendExternalTask() {
    return Tasks
        .oneTime(suspend_external_TASK)
        .execute(getExternalTaskInstance(suspend_external_TASK));
  }

  @Bean
  Task<Void> resumeExternalTask() {
    return Tasks
        .oneTime(resume_external_TASK)
        .execute(getExternalTaskInstance(resume_external_TASK));
  }

  @Bean
  Task<Void> disconnectExternalTask() {
    return Tasks
        .oneTime(disconnect_external_TASK)
        .execute(getExternalTaskInstance(disconnect_external_TASK));
  }

  @Bean
  Task<Void> disconnectExternalExternalTask() {
    return Tasks
        .oneTime(disconnect_external_external_TASK)
        .execute(getExternalTaskInstance(disconnect_external_external_TASK));
  }

  @Bean
  Task<Void> changePriceExternalTask() {
    return Tasks
        .oneTime(change_price_external_TASK)
        .execute(getExternalTaskInstance(change_price_external_TASK));
  }

  private VoidExecutionHandler<Void> getTaskInstance(TaskWithoutDataDescriptor descriptor) {
    return (instance, ctx) -> {
      Event event = internalEventBuilder(instance, ctx).build();
      fsmApp.sendEvent(event);
    };
  }

  private VoidExecutionHandler<Void> getExternalTaskInstance(TaskWithoutDataDescriptor descriptor) {
    return (instance, ctx) -> {
      log.info("external task instance run. productId: {}, taskName: {}", instance.getId(), instance.getTaskName());
      // TODO: send request to OE
    };
  }
}

