package io.mambatech.mambasplit.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record ErrorResponse(String code, String message, String timestamp) {}

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("INVALID_REQUEST", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validationError(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Request validation failed.";
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("VALIDATION_FAILED", message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Request validation failed.";
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("VALIDATION_FAILED", message));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> dataIntegrityViolation(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(error("DATA_INTEGRITY_VIOLATION", "Resource conflicts with an existing record."));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> dataAccessError(DataAccessException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error("DATA_ACCESS_ERROR", "A data access error occurred."));
  }

  private ErrorResponse error(String code, String message) {
    return new ErrorResponse(code, message, Instant.now().toString());
  }
}
