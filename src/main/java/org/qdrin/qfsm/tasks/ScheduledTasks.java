package org.qdrin.qfsm.tasks;

import java.time.Duration;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ScheduledTasks {

  private Schedule fixedDelay(Duration interval) {
    return FixedDelay.of(interval);
  }

  @Bean
  Duration eagerInterval() {
    return Duration.ofSeconds(10);
  }

  @Bean
  Task<Void> dumbLogging(Duration eagerInterval) {
    return Tasks
        .recurring("dumb-logging", fixedDelay(eagerInterval))
        .execute((instance, ctx) -> log.info("Hi, I log this statement every {}", eagerInterval));
  }
}

