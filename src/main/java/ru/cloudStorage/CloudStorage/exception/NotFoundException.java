package ru.cloudStorage.CloudStorage.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String path) {
        super(path);
    }
}
