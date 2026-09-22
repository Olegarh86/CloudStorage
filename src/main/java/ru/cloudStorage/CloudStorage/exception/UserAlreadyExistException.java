package ru.cloudStorage.CloudStorage.exception;

public class UserAlreadyExistException extends AlreadyExistException {
    public UserAlreadyExistException(String userName) {
        super("User with name: " + userName + " already exist, change another name");
    }
}
