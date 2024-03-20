package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ResumingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("ResumingEntry started. event: {}, message: {}", context.getEvent(), context.getMessage());
    ExtendedState extendedState = context.getExtendedState();
    Product product = extendedState.get("product", Product.class);
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    Consumer<TaskContext> taskFunc = ScheduledTasks::startResumeExternalTask;
    TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), Instant.now());
    taskFunc.accept(ctx);
  }
}
