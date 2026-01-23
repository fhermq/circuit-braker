package com.ecommerce.client;

import com.ecommerce.exception.CircuitBreakerOpenException;
import com.ecommerce.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class NotificationServiceClient {
    private final Random random = new Random();
    private boolean simulateFailure = false;

    @CircuitBreaker(name = "notificationService", fallbackMethod = "notificationFallback")
    public boolean sendNotification(String orderId, String customerId, String message) {
        log.info("Sending notification for order: {}, customer: {}", orderId, customerId);
        
        if (simulateFailure) {
            throw new ServiceException("NotificationService", "NOTIFICATION_FAILED", 
                    "Notification sending failed for order: " + orderId);
        }

        // Simulate occasional slow responses
        if (random.nextInt(100) < 10) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Notification sent successfully for order: {}", orderId);
        return true;
    }

    public boolean notificationFallback(String orderId, String customerId, String message, Exception ex) {
        log.warn("Notification service fallback triggered for order: {}. Reason: {}", orderId, ex.getMessage());
        if (ex instanceof CircuitBreakerOpenException) {
            throw (CircuitBreakerOpenException) ex;
        }
        // For notifications, we can be more lenient - log and continue
        log.info("Notification service unavailable, but order processing continues for order: {}", orderId);
        return false;
    }

    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure = simulate;
        log.info("Notification service failure simulation set to: {}", simulate);
    }
}
