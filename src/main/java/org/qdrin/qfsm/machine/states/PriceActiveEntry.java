package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import org.qdrin.qfsm.tasks.ActionSuit;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;
import java.util.List;

import javax.sql.DataSource;

import org.qdrin.qfsm.machine.actions.AddActionAction;
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
    Product product = context.getExtendedState().get("product", Product.class);
    ProductPrice nextPrice = context.getStateMachine().getExtendedState().get("nextPrice", ProductPrice.class);
    if(! context.getEvent().equals("complete_price")) {
      OffsetDateTime activeEndDate = nextPrice.getNextPayDate();
      activeEndDate.minus(priceEndedBefore);
      product.setActiveEndDate(activeEndDate);
      if(nextPrice.getProductStatus().equals("ACTIVE_TRIAL")) {
        product.setTrialEndDate(activeEndDate);
      }
      log.debug("activeEndDate: {}, trialEndDate: {}, priceEndedBefore: {}",
          product.getActiveEndDate(), product.getTrialEndDate(), priceEndedBefore);
      new AddActionAction(ActionSuit.PRICE_ENDED
          .withWakeAt(activeEndDate.minus(priceEndedBefore)))
          .execute(context);
      // TODO: Change direct task creation to post action variable here and everywhere
      // var postActions = context.getExtendedState().get("postActions", PostActions);
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
      context.getStateMachine().getExtendedState().getVariables().remove("nextPrice");
    }
  }
}
