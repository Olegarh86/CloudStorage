package ru.cloudStorage.CloudStorage.exception;

public class ObjectStreamException extends RuntimeException {
    public ObjectStreamException(String message) {
        super(message);
    }

    public ObjectStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
