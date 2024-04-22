package org.qdrin.qfsm.exception;

public class BadUserDataException extends RuntimeException {

  public BadUserDataException() {}

  public BadUserDataException(String message) {
    super(message);
  }

  public BadUserDataException(Throwable th) {
    super(th);
  }

  public BadUserDataException(String message, Throwable th) {
    super(message, th);
  }
}
