package com.ecommerce.client;

import com.ecommerce.exception.CircuitBreakerOpenException;
import com.ecommerce.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Slf4j
@Component
public class PaymentServiceClient {
    private final Random random = new Random();
    private boolean simulateFailure = false;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public boolean processPayment(String orderId, String customerId, BigDecimal amount) {
        log.info("Processing payment for order: {}, customer: {}, amount: {}", orderId, customerId, amount);
        
        if (simulateFailure) {
            throw new ServiceException("PaymentService", "PAYMENT_FAILED", 
                    "Payment processing failed for order: " + orderId);
        }

        // Simulate occasional slow responses
        if (random.nextInt(100) < 20) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Payment processed successfully for order: {}", orderId);
        return true;
    }

    public boolean paymentFallback(String orderId, String customerId, BigDecimal amount, Exception ex) {
        log.warn("Payment service fallback triggered for order: {}. Reason: {}", orderId, ex.getMessage());
        if (ex instanceof CircuitBreakerOpenException) {
            throw (CircuitBreakerOpenException) ex;
        }
        throw new ServiceException("PaymentService", "FALLBACK_EXECUTED", 
                "Payment service unavailable, fallback executed for order: " + orderId);
    }

    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure = simulate;
        log.info("Payment service failure simulation set to: {}", simulate);
    }
}
