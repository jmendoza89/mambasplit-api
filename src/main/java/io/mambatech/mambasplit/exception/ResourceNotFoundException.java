package io.mambatech.mambasplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Automatically returns HTTP 404 response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(String resourceType, String identifier) {
    super("RESOURCE_NOT_FOUND", 
          String.format("%s not found: %s", resourceType, identifier));
  }
  
  public ResourceNotFoundException(String message) {
    super("RESOURCE_NOT_FOUND", message);
  }
}
