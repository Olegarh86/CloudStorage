package ru.cloudStorage.CloudStorage.exception;

public class RemoveObjectException extends RuntimeException {
    public RemoveObjectException(String message) {
    }

    public RemoveObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
