package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceActiveEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceActiveEntry started. event: {}", context.getEvent());
    new SignalAction("prolong").execute(context);
    Product product = context.getExtendedState().get("product", Product.class);
    ProductPrice nextPrice = context.getStateMachine().getExtendedState().get("nextPrice", ProductPrice.class);
    if(! context.getEvent().equals("complete_price")) {
      OffsetDateTime activeEndDate = nextPrice.getNextPayDate();
      product.setActiveEndDate(activeEndDate);
      // TODO: Change direct task creation to post action variable here and everywhere
      // var postActions = context.getExtendedState().get("postActions", PostActions);
      // postActions.addNewTask("startPriceEndedTask", product.getProductId(), activeEndDate);
////////////////////////////////////////////////////////////////////////////////////////////////////////
      final SchedulerClient schedulerClient =
        SchedulerClient.Builder.create(dataSource)
            .serializer(new JacksonSerializer())
            .build();
      Consumer<TaskContext> priceEndedFunc = ScheduledTasks::startPriceEndedTask;
      TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), activeEndDate.toInstant());
      priceEndedFunc.accept(ctx);
/////////////////////////////////////////////////////////////////////////////////////////////////////////
      context.getStateMachine().getExtendedState().getVariables().remove("nextPrice");
    }
  }
}
