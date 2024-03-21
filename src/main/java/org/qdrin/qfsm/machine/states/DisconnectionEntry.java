package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.DeleteTaskAction;
import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
public class DisconnectionEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    Product product = context.getExtendedState().get("product", Product.class);
    log.debug("DisconnectionEntry started. event: {}, message: {}", context.getEvent());
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    Consumer<TaskContext> taskFunc = ScheduledTasks::startDisconnectExternalTask;
    // TODO: Add characteristics analysis
    TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), Instant.now());
    taskFunc.accept(ctx);
  }
}
