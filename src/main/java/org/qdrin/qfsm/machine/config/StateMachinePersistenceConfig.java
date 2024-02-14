package org.qdrin.qfsm.machine.config;

import org.qdrin.qfsm.repository.QStateMachineRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;

@Configuration
public class StateMachinePersistenceConfig {

    // Нужен ли этот bean?
    @Bean
    public QStateMachineRepository workaroundBean() {
        return new QStateMachineRepository();
    }

    @Bean
    public JpaPersistingStateMachineInterceptor<String,String, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }

}
