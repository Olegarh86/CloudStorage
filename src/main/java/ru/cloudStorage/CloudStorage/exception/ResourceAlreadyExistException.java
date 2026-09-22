package ru.cloudStorage.CloudStorage.exception;

public class ResourceAlreadyExistException extends AlreadyExistException {
    public ResourceAlreadyExistException(String path) {
        super("The resource lying on the path: " + path + " to already exists");
    }
}
