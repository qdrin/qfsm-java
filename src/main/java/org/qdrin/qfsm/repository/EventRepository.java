package org.qdrin.qfsm.repository;

import org.qdrin.qfsm.model.Event;
import org.springframework.data.repository.CrudRepository;

public interface EventRepository extends CrudRepository<Event, String> {
  Event findByRefIdAndSourceCodeAndEventType(String refId, String sourceCode, String eventType);
}