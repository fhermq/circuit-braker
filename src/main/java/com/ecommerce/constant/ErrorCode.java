package com.ecommerce.constant;

public class ErrorCode {
    // Order Service Errors
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String INVALID_ORDER_INPUT = "INVALID_ORDER_INPUT";

    // Payment Service Errors
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_SERVICE_UNAVAILABLE = "PAYMENT_SERVICE_UNAVAILABLE";

    // Inventory Service Errors
    public static final String INVENTORY_FAILED = "INVENTORY_FAILED";
    public static final String INVENTORY_SERVICE_UNAVAILABLE = "INVENTORY_SERVICE_UNAVAILABLE";

    // Notification Service Errors
    public static final String NOTIFICATION_FAILED = "NOTIFICATION_FAILED";
    public static final String NOTIFICATION_SERVICE_UNAVAILABLE = "NOTIFICATION_SERVICE_UNAVAILABLE";

    // Circuit Breaker Errors
    public static final String CIRCUIT_BREAKER_OPEN = "CIRCUIT_BREAKER_OPEN";

    private ErrorCode() {
        throw new AssertionError("Cannot instantiate ErrorCode");
    }
}
