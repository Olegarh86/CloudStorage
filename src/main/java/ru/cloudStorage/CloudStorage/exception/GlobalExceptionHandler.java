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
        log.warn("Invalid data ", e);
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(errors));
    }

    @ExceptionHandler(ValidateException.class)
    public ResponseEntity<ErrorMessage> unacceptableFolderNameException(ValidateException e) {
        log.warn("Invalid data ", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(
                "Incorrect folder name: '" + e.getMessage() + "'. The folder name cannot contain the following " +
                "characters: '/\\:*?\"<>|', length must be from 1 to 200 characters, and last character must be '/'"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAAuthException(AuthenticationException e) {
        log.warn("User not authenticated ", e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage("Invalid username or password"));
    }

    @ExceptionHandler(CreateNewFolderException.class)
    public ResponseEntity<ErrorMessage> createNewFolderException(CreateNewFolderException e) {
        log.warn("Folder not created ", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Folder not created"));
    }

    @ExceptionHandler(ObjectNotExistException.class)
    public ResponseEntity<ErrorMessage> objectNotExistException(ObjectNotExistException e) {
        log.warn("Object not exist ", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage("Object not found: '" + e.getMessage() + "'"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorMessage> handleNotFoundException(NotFoundException e) {
        log.warn("Object not found ", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage()));
    }

    @ExceptionHandler(AlreadyExistException.class)
    public ResponseEntity<ErrorMessage> handleUserAlreadyExistException(AlreadyExistException e) {
        log.warn("Object already exist ", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleException(Exception e) {
        log.error("Internal Server Error ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessage("Internal Server Error"));
    }
}
