package io.mambatech.mambasplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when business validation rules are violated.
 * Automatically returns HTTP 400 response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends BusinessException {
  public ValidationException(String message) {
    super("VALIDATION_FAILED", message);
  }
  
  public ValidationException(String field, String constraint) {
    super("VALIDATION_FAILED", 
          String.format("%s %s", field, constraint));
  }
}
