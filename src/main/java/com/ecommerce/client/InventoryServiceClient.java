package com.ecommerce.client;

import com.ecommerce.exception.CircuitBreakerOpenException;
import com.ecommerce.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class InventoryServiceClient {
    private final Random random = new Random();
    private boolean simulateFailure = false;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    public boolean reserveInventory(String orderId, String productId, Integer quantity) {
        log.info("Reserving inventory for order: {}, product: {}, quantity: {}", orderId, productId, quantity);
        
        if (simulateFailure) {
            throw new ServiceException("InventoryService", "INVENTORY_FAILED", 
                    "Inventory reservation failed for order: " + orderId);
        }

        // Simulate occasional slow responses
        if (random.nextInt(100) < 15) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Inventory reserved successfully for order: {}", orderId);
        return true;
    }

    public boolean inventoryFallback(String orderId, String productId, Integer quantity, Exception ex) {
        log.warn("Inventory service fallback triggered for order: {}. Reason: {}", orderId, ex.getMessage());
        if (ex instanceof CircuitBreakerOpenException) {
            throw (CircuitBreakerOpenException) ex;
        }
        throw new ServiceException("InventoryService", "FALLBACK_EXECUTED", 
                "Inventory service unavailable, fallback executed for order: " + orderId);
    }

    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure = simulate;
        log.info("Inventory service failure simulation set to: {}", simulate);
    }
}
