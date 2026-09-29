package ru.cloudStorage.CloudStorage.exception;

public class UploadObjectException extends RuntimeException {
    public UploadObjectException(String message) {
        super(message);
    }

    public UploadObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
