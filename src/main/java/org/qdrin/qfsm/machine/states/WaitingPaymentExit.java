package org.qdrin.qfsm.machine.states;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WaitingPaymentExit implements Action<String, String> {

  @Autowired
  DataSource dataSource;

  @Override
  public void execute(StateContext<String, String> context) {
    log.debug("WaitingPaymentExit started. event: {}", context.getEvent());
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
          .serializer(new JacksonSerializer())
          .build();
    schedulerClient.getScheduledExecutions().stream()
        .filter(s -> s.getTaskInstance().getTaskName().equals("waiting_pay_ended"))
        .findAny()
        .ifPresent(s -> schedulerClient.cancel(s.getTaskInstance()));
  }
}
