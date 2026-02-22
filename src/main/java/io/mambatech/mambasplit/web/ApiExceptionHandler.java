package io.mambatech.mambasplit.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record ErrorResponse(String message, String timestamp) {}

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validationError(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Request validation failed.";
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Request validation failed.";
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(message));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> dataAccessError(DataAccessException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error("A data access error occurred."));
  }

  private ErrorResponse error(String message) {
    return new ErrorResponse(message, Instant.now().toString());
  }
}
