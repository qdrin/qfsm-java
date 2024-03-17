package org.qdrin.qfsm.machine.actions;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;

import reactor.core.publisher.Mono;

public class DeleteTaskAction implements Action<String, String> {
  DataSource dataSource;

  private String taskName;
  public DeleteTaskAction(String taskName, DataSource dataSource) {
    this.taskName = taskName;
    this.dataSource = dataSource;
  }
  @Override
  public void execute(StateContext<String, String> context) {
    final SchedulerClient schedulerClient =
      SchedulerClient.Builder.create(dataSource)
        .serializer(new JacksonSerializer())
        .build();
    schedulerClient.getScheduledExecutions().stream()
        .filter(s -> s.getTaskInstance().getTaskName().equals(taskName))
        .findAny()
        .ifPresent(s -> schedulerClient.cancel(s.getTaskInstance()));
  }
}
