package org.qdrin.qfsm.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.qdrin.qfsm.persist.ProductStateMachinePersist;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachineException;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.AbstractStateMachine;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of a {@link StateMachineService}.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */

@Slf4j
public class QStateMachineService<S, E> implements StateMachineService<S, E>, DisposableBean {

	// private final static Log log = LogFactory.getLog(QStateMachineService.class);
	private final StateMachineFactory<S, E> stateMachineFactory;
	private final Map<String, StateMachine<S, E>> machines = new HashMap<String, StateMachine<S, E>>();
	private StateMachinePersister<S, E, String> stateMachinePersister;

	/**
	 * Instantiates a new default state machine service.
	 *
	 * @param stateMachineFactory the state machine factory
	 */
	public QStateMachineService(StateMachineFactory<S, E> stateMachineFactory) {
		this(stateMachineFactory, null);
	}

	/**
	 * Instantiates a new default state machine service.
	 *
	 * @param stateMachineFactory the state machine factory
	 * @param stateMachinePersist the state machine persist
	 */
	public QStateMachineService(StateMachineFactory<S, E> stateMachineFactory,
		StateMachinePersister<S, E, String> stateMachinePersister) {
		Assert.notNull(stateMachineFactory, "'stateMachineFactory' must be set");
		this.stateMachineFactory = stateMachineFactory;
		this.stateMachinePersister = stateMachinePersister;
	}

	@Override
	public final void destroy() throws Exception {
		doStop();
	}

	@Override
	public StateMachine<S, E> acquireStateMachine(String machineId) {
		return acquireStateMachine(machineId, true);
	}

	@Override
	public StateMachine<S, E> acquireStateMachine(String machineId, boolean start) {
		log.debug("Acquiring machine with id " + machineId);
		StateMachine<S, E> stateMachine;
		// naive sync to handle concurrency with release
		synchronized (machines) {
			stateMachine = machines.get(machineId);
			if (stateMachine == null) {
				log.debug("Getting new machine from factory with id " + machineId);
				stateMachine = stateMachineFactory.getStateMachine(machineId);
				try {
					stateMachinePersister.restore(stateMachine, machineId);
					((AbstractStateMachine<S, E>) stateMachine).setId(machineId);
				} catch (Exception e) {
					log.error("Cannot restore stateMachineId: '{}': {}", machineId, e.getLocalizedMessage());
					return null;
				}
				machines.put(machineId, stateMachine);
			}
		}
		// handle start outside of sync as it might take some time and would block other machines acquire
		return handleStart(stateMachine, start);
	}

	@Override
	public void releaseStateMachine(String machineId) {
		releaseStateMachine(machineId, true);
	}

	@Override
	public void releaseStateMachine(String machineId, boolean stop) {
		log.info("Releasing machine with id " + machineId);
		synchronized (machines) {
			StateMachine<S, E> stateMachine = machines.remove(machineId);
			if (stateMachine != null) {
				log.info("Found machine with id " + machineId);
				try {
					stateMachinePersister.persist(stateMachine, machineId);
				} catch (Exception e) {
					log.error("Cannot persist stateMachineId: '{}': {}", machineId, e.getLocalizedMessage());
					e.printStackTrace();
				}
				handleStop(stateMachine, stop);
			}
		}
	}

	/**
	 * Determines if the given machine identifier denotes a known managed state machine.
	 *
	 * @param machineId machine identifier
	 * @return true if machineId denotes a known managed state machine currently in memory
	 */
	public boolean hasStateMachine(String machineId) {
		synchronized (machines) {
			return machines.containsKey(machineId);
		}
	}

	/**
	 * Sets the state machine persist.
	 *
	 * @param stateMachinePersist the state machine persist
	 */
	public void setStateMachinePersist(StateMachinePersister<S, E, String> stateMachinePersister) {
		this.stateMachinePersister = stateMachinePersister;
	}

	protected void doStop() {
		log.info("Entering stop sequence, stopping all managed machines");
		synchronized (machines) {
			ArrayList<String> machineIds = new ArrayList<>(machines.keySet());
			for (String machineId : machineIds) {
				releaseStateMachine(machineId, true);
			}
		}
	}

	protected StateMachine<S, E> restoreStateMachine(StateMachine<S, E> stateMachine, final StateMachineContext<S, E> stateMachineContext) {
		if (stateMachineContext == null) {
			return stateMachine;
		}
		stateMachine.stopReactively().block();
		stateMachine.getStateMachineAccessor().doWithAllRegions(function -> function.resetStateMachineReactively(stateMachineContext).block());
		return stateMachine;
	}

	protected StateMachine<S, E> handleStart(StateMachine<S, E> stateMachine, boolean start) {
		if (start) {
			if (!((Lifecycle) stateMachine).isRunning()) {
				StartListener<S, E> listener = new StartListener<>(stateMachine);
				stateMachine.addStateListener(listener);
				stateMachine.startReactively().block();
				try {
					listener.latch.await();
				} catch (InterruptedException e) {
				}
			}
		}
		return stateMachine;
	}

	protected StateMachine<S, E> handleStop(StateMachine<S, E> stateMachine, boolean stop) {
		if (stop) {
			if (((Lifecycle) stateMachine).isRunning()) {
				StopListener<S, E> listener = new StopListener<>(stateMachine);
				stateMachine.addStateListener(listener);
				stateMachine.stopReactively().block();
				try {
					listener.latch.await();
				} catch (InterruptedException e) {
				}
			}
		}
		return stateMachine;
	}

	private static class StartListener<S, E> extends StateMachineListenerAdapter<S, E> {

		final CountDownLatch latch = new CountDownLatch(1);
		final StateMachine<S, E> stateMachine;

		public StartListener(StateMachine<S, E> stateMachine) {
			this.stateMachine = stateMachine;
		}

		@Override
		public void stateMachineStarted(StateMachine<S, E> stateMachine) {
			this.stateMachine.removeStateListener(this);
			latch.countDown();
		}
	}

	private static class StopListener<S, E> extends StateMachineListenerAdapter<S, E> {

		final CountDownLatch latch = new CountDownLatch(1);
		final StateMachine<S, E> stateMachine;

		public StopListener(StateMachine<S, E> stateMachine) {
			this.stateMachine = stateMachine;
		}

		@Override
		public void stateMachineStopped(StateMachine<S, E> stateMachine) {
			this.stateMachine.removeStateListener(this);
			latch.countDown();
		}
	}
}
