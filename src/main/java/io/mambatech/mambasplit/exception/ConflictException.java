package io.mambatech.mambasplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a resource already exists and conflicts with the request.
 * Automatically returns HTTP 409 response.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super("CONFLICT", message);
  }
  
  public ConflictException(String resourceType, String reason) {
    super("CONFLICT", 
          String.format("%s: %s", resourceType, reason));
  }
}
