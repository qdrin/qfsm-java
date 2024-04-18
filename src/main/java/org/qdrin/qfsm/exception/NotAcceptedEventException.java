package org.qdrin.qfsm.exception;

public class NotAcceptedEventException extends RuntimeException {

  public NotAcceptedEventException() {}

  public NotAcceptedEventException(String message) {
    super(message);
  }

  public NotAcceptedEventException(Throwable th) {
    super(th);
  }

  public NotAcceptedEventException(String message, Throwable th) {
    super(message, th);
  }
}
