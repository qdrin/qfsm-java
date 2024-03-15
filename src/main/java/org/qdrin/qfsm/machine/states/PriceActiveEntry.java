package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;

import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceActiveEntry implements Action<String, String> {

  @Autowired
  SchedulerClient schedulerClient;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceActiveEntry started. event: {}", context.getEvent());
    new SignalAction("prolong").execute(context);
    Product product = context.getExtendedState().get("product", Product.class);
    ProductPrice nextPrice = context.getStateMachine().getExtendedState().get("nextPrice", ProductPrice.class);
    if(! context.getEvent().equals("complete_price")) {
      product.setActiveEndDate(nextPrice.getNextPayDate());
      Consumer<TaskContext> priceEndedFunc = ScheduledTasks::startPriceEndedTask;
      Consumer<TaskContext> changePriceFunc = ScheduledTasks::startChangePriceTask;
      TaskContext ctx = new TaskContext();
      ctx.id = product.getProductId();
      ctx.schedulerClient = schedulerClient;
      ctx.wakeAt = product.getActiveEndDate().toInstant();
      priceEndedFunc.accept(ctx);
      changePriceFunc.accept(ctx);
      context.getStateMachine().getExtendedState().getVariables().remove("nextPrice");
    }
  }
}
