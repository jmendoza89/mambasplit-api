package io.mambatech.mambasplit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when authentication fails (invalid credentials, expired tokens, etc.).
 * Automatically returns HTTP 401 response.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationException extends BusinessException {
  public AuthenticationException(String message) {
    super("AUTHENTICATION_FAILED", message);
  }
}
