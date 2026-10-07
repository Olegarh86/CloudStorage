package ru.cloudStorage.CloudStorage.exception;

public class ObjectNotExistException extends RuntimeException {
    public ObjectNotExistException(String message) {
        super("Object not found: '" + message + "'");
    }
    public ObjectNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
