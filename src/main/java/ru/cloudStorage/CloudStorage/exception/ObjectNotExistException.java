package ru.cloudStorage.CloudStorage.exception;

public class ObjectNotExistException extends RuntimeException {
    public ObjectNotExistException(String message) {
        super(message);
    }
}
