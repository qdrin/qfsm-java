package org.qdrin.qfsm.machine.states;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.qdrin.qfsm.model.Product;
import org.qdrin.qfsm.model.ProductPrice;
import org.qdrin.qfsm.tasks.ExternalData;
import org.qdrin.qfsm.tasks.ScheduledTasks;
import org.qdrin.qfsm.tasks.ScheduledTasks.TaskContext;
import org.qdrin.qfsm.utils.PriceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import org.qdrin.qfsm.machine.actions.SignalAction;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PriceChangingEntry implements Action<String, String> {

  @Autowired
  DataSource dataSource;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceChangingEntry started. event: {}, message: {}", context.getEvent(), context.getMessage());
    Product product = context.getExtendedState().get("product", Product.class);
    Map<Object, Object> variables = context.getExtendedState().getVariables();
    int tPeriod = product.getTarificationPeriod();
    if (tPeriod == 0) {
      ProductPrice price = PriceHelper.getProductPrice(context);
      variables.put("nextPrice", price);
      SignalAction changePrice = new SignalAction("change_price");
      changePrice.execute(context);
      if(price.getProductStatus().equals("ACTIVE_TRIAL")) {
        log.debug("First trial period. Sending auto 'payment_processed'");
        SignalAction paymentProcessed = new SignalAction("payment_processed");
        paymentProcessed.execute(context);
      }
    } else {
      // TODO: This is emulator of price-calculator request. Remove after develop
      ProductPrice plannedPrice = ExternalData.RequestProductPrice();
      variables.put("plannedPrice", plannedPrice);
      ///////////////////////////////////////////////////////////////////////////
      
      final SchedulerClient schedulerClient =
        SchedulerClient.Builder.create(dataSource)
            .serializer(new JacksonSerializer())
            .build();
      Consumer<TaskContext> taskFunc = ScheduledTasks::startChangePriceTask;
      TaskContext ctx = new TaskContext(schedulerClient, product.getProductId(), Instant.now());
      taskFunc.accept(ctx);
      // ProductPrice nextPrice = ExternalData.RequestProductPrice();
      // variables.put("nextPrice", nextPrice);
    }
  }
}
