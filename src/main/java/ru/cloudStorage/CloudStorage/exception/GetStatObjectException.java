package ru.cloudStorage.CloudStorage.exception;

public class GetStatObjectException extends RuntimeException {
  public GetStatObjectException(String message) {
  }

  public GetStatObjectException(String message, Throwable cause) {
    super(message, cause);
  }
}
