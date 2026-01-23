package com.ecommerce.exception;

public class ServiceException extends RuntimeException {
    private final String serviceName;
    private final String errorCode;

    public ServiceException(String serviceName, String errorCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public ServiceException(String serviceName, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
