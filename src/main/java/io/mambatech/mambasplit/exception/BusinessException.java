package io.mambatech.mambasplit.exception;

/**
 * Base exception for all application-level business logic exceptions.
 * Provides consistent error handling across the application.
 */
public abstract class BusinessException extends RuntimeException {
  private final String errorCode;
  
  protected BusinessException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
  
  protected BusinessException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
  
  public String getErrorCode() {
    return errorCode;
  }
}
