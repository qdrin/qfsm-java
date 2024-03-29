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
public class PriceChangingExit implements Action<String, String> {

  @Autowired
  DataSource dataSource;
  
  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("PriceChangingExit started. event: {}, message: {}", context.getEvent(), context.getMessage());
    Product product = context.getExtendedState().get("product", Product.class);
    // При выходе из Suspended мы обнуляем прайс, а здесь выставляем его равным nextPrice
    if(product.getProductPrices() == null) {
      ProductPrice nextPrice = context.getExtendedState().get("nextPrice", ProductPrice.class);
      product.setProductPrices(nextPrice);
    }
  }
}
