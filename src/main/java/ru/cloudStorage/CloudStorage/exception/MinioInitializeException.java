package ru.cloudStorage.CloudStorage.exception;

public class MinioInitializeException extends RuntimeException {
    public MinioInitializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
