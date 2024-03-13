package org.qdrin.qfsm.persist;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.data.jpa.JpaRepositoryStateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.util.Assert;

/**
 * {@code JPA} implementation of a {@link AbstractPersistingStateMachineInterceptor}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 * @param <T> the type of persister context object
 */
public class QJpaPersistingStateMachineInterceptor<S, E, T> extends QAbstractPersistingStateMachineInterceptor<S, E, T>
		implements StateMachineRuntimePersister<S, E, T> {

	private final JpaRepositoryStateMachinePersist<S, E> persist;

	/**
	 * Instantiates a new jpa persisting state machine interceptor.
	 *
	 * @param jpaStateMachineRepository the jpa state machine repository
	 */
	public QJpaPersistingStateMachineInterceptor(JpaStateMachineRepository jpaStateMachineRepository) {
		Assert.notNull(jpaStateMachineRepository, "'jpaStateMachineRepository' must be set");
		this.persist = new JpaRepositoryStateMachinePersist<S, E>(jpaStateMachineRepository);
	}

	/**
	 * Instantiates a new jpa persisting state machine interceptor.
	 *
	 * @param persist the persist
	 */
	public QJpaPersistingStateMachineInterceptor(JpaRepositoryStateMachinePersist<S, E> persist) {
		Assert.notNull(persist, "'persist' must be set");
		this.persist = persist;
	}

	@Override
	public StateMachineInterceptor<S, E> getInterceptor() {
		return this;
	}

	@Override
	public void write(StateMachineContext<S, E> context, T contextObj) throws Exception {
		persist.write(context, contextObj);
	}

	@Override
	public StateMachineContext<S, E> read(Object contextObj) throws Exception {
		return persist.read(contextObj);
	}
}
