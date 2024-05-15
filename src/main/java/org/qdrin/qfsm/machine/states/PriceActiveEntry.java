package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import org.qdrin.qfsm.tasks.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.SignalAction;
import org.qdrin.qfsm.model.*;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceActiveEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Value("${application.fsm.time.priceEndedBefore}")
  Duration priceEndedBefore;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("event: {}", context.getEvent());
    new SignalAction("prolong").execute(context);
    ExtendedState extendedState = context.getStateMachine().getExtendedState();
    Product product = extendedState.get("product", Product.class);
    ProductPrice nextPrice = extendedState.get("nextPrice", ProductPrice.class);
    if(! context.getEvent().equals("complete_price")) {
      OffsetDateTime activeEndDate = nextPrice.getNextPayDate();
      product.setActiveEndDate(activeEndDate);
      List<Product> components = (List<Product>) extendedState.getVariables().get("components");
      components.stream().filter(c -> ! c.getMachineContext().getIsIndependent()).forEach((c) -> {c.setActiveEndDate(activeEndDate);});

      if(nextPrice.getProductStatus().equals("ACTIVE_TRIAL")) {
        product.setTrialEndDate(activeEndDate);
        components.stream().filter(c -> ! c.getMachineContext().getIsIndependent()).forEach((c) -> {c.setTrialEndDate(activeEndDate);});
      }
      log.debug("activeEndDate: {}, trialEndDate: {}, priceEndedBefore: {}",
          product.getActiveEndDate(), product.getTrialEndDate(), priceEndedBefore);
      TaskPlan tasks = extendedState.get("tasks", TaskPlan.class);
      tasks.addToCreatePlan(TaskDef.builder()
        .type(TaskType.PRICE_ENDED)
        .wakeAt(activeEndDate.minus(priceEndedBefore))
        .build()  
      );
      // TODO: Change direct task creation to post action variable here and everywhere
      // var postActions = context.getStateMachine().getExtendedState().get("postActions", PostActions);
      // postActions.addNewTask("startPriceEndedTask", product.getProductId(), activeEndDate);
////////////////////////////////////////////////////////////////////////////////////////////////////////
      // final SchedulerClient schedulerClient =
      //   SchedulerClient.Builder.create(dataSource)
      //       .serializer(new JacksonSerializer())
      //       .build();
      // Consumer<TaskContext> priceEndedFunc = ScheduledTasks::startPriceEndedTask;
      // TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), activeEndDate.toInstant());
      // priceEndedFunc.accept(ctx);
/////////////////////////////////////////////////////////////////////////////////////////////////////////
      extendedState.getVariables().remove("nextPrice");
    }
  }
}
