package com.ecommerce.exception;

public class CircuitBreakerOpenException extends ServiceException {
    public CircuitBreakerOpenException(String serviceName) {
        super(serviceName, "CIRCUIT_BREAKER_OPEN", 
              "Circuit breaker is OPEN for service: " + serviceName + ". Service is temporarily unavailable.");
    }
}
