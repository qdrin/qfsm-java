package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;

import java.time.Instant;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendedEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendedEntry started. event: {}", context.getEvent());
    Product product = context.getExtendedState().get("product", Product.class);
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    Consumer<TaskContext> suspendEndedFunc = ScheduledTasks::startSuspendEndedTask;
    // TODO: substitube wakeAt with configurable Instant value
    TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), Instant.now().plusSeconds(30*86400));
    suspendEndedFunc.accept(ctx);
  }
}
