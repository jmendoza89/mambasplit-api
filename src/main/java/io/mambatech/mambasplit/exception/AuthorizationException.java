package io.mambatech.mambasplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user is not authorized to perform an action.
 * Automatically returns HTTP 403 response.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AuthorizationException extends BusinessException {
  public AuthorizationException(String message) {
    super("FORBIDDEN", message);
  }
  
  public AuthorizationException(String action, String resource) {
    super("FORBIDDEN", 
          String.format("Not authorized to %s %s", action, resource));
  }
}
