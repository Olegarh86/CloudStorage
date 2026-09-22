package ru.cloudStorage.CloudStorage.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.cloudStorage.CloudStorage.dto.ErrorMessage;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleValidationException(MethodArgumentNotValidException e) {
        log.warn(e.getMessage(), e);
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(errors));
    }

    @ExceptionHandler(ValidateException.class)
    public ResponseEntity<ErrorMessage> unacceptableFolderNameException(ValidateException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(
                "Incorrect folder name: '" + e.getMessage() + "'. The folder name cannot contain the following " +
                "characters: '/\\:*?\"<>|', length must be from 1 to 200 characters, and last character must be '/'"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAAuthException(AuthenticationException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Invalid username or password"));
    }

    @ExceptionHandler(CreateNewFolderException.class)
    public ResponseEntity<ErrorMessage> createNewFolderException(CreateNewFolderException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Folder not found"));
    }

    @ExceptionHandler(ObjectNotExistException.class)
    public ResponseEntity<ErrorMessage> objectNotExistException(ObjectNotExistException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Object " + e.getMessage() + " not " +
                                                                                 "found"));
    }

    @ExceptionHandler(NotFoundException.class) // TODO For resource not found
    public ResponseEntity<ErrorMessage> handleNotFoundException(NotFoundException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Resource not found"));
    }

    @ExceptionHandler(AlreadyExistException.class)
    public ResponseEntity<ErrorMessage> handleUserAlreadyExistException(AlreadyExistException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Internal Server Error"));
    }
}
