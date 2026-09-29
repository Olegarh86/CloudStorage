package ru.cloudStorage.CloudStorage.exception;

public class CopyObjectException extends RuntimeException {
    public CopyObjectException(String message) {
    }

    public CopyObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
