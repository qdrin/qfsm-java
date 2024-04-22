package org.qdrin.qfsm.exception;

public class RepeatedEventException extends RuntimeException {

  public RepeatedEventException() {}

  public RepeatedEventException(String message) {
    super(message);
  }

  public RepeatedEventException(Throwable th) {
    super(th);
  }

  public RepeatedEventException(String message, Throwable th) {
    super(message, th);
  }
}
