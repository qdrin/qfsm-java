package org.qdrin.qfsm.exception;

public class EventDeniedException extends RuntimeException {

  public EventDeniedException() {}

  public EventDeniedException(String message) {
    super(message);
  }

  public EventDeniedException(Throwable th) {
    super(th);
  }

  public EventDeniedException(String message, Throwable th) {
    super(message, th);
  }
}
