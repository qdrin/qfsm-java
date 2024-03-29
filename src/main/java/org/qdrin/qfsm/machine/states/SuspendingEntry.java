package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SuspendingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("SuspendingEntry started. event: {}, message: {}", context.getEvent());
    ProductPrice price = PriceHelper.getProductPrice(context);
    price.setPeriod(0);
    log.debug("SuspendingEntry productPrice: {}", price);
    PriceHelper.setProductPrice(context, price);
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    Consumer<TaskContext> taskFunc = ScheduledTasks::startSuspendExternalTask;
    // TODO: Add characteristics analysis
    TaskContext ctx = new TaskContext(schedulerClient, context.getStateMachine().getId(), Instant.now());
    taskFunc.accept(ctx);
  }
}
