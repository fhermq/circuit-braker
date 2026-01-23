package com.ecommerce.service;

import com.ecommerce.client.InventoryServiceClient;
import com.ecommerce.client.NotificationServiceClient;
import com.ecommerce.client.PaymentServiceClient;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.CircuitBreakerOpenException;
import com.ecommerce.exception.ServiceException;
import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional
    public Order createOrder(OrderRequest request) {
        validateOrderRequest(request);
        
        String orderId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Order order = Order.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .status(Order.OrderStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: orderId={}, customerId={}, amount={}", 
            orderId, request.getCustomerId(), request.getTotalAmount());

        try {
            processOrder(savedOrder);
        } catch (CircuitBreakerOpenException ex) {
            log.error("Circuit breaker open for service: {}", ex.getServiceName());
            savedOrder.setStatus(Order.OrderStatus.FAILED);
            savedOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
            throw ex;
        } catch (ServiceException ex) {
            log.error("Service error: {}", ex.getMessage());
            savedOrder.setStatus(Order.OrderStatus.FAILED);
            savedOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
            throw ex;
        }

        return savedOrder;
    }

    @Transactional
    private void processOrder(Order order) {
        try {
            // Step 1: Process Payment
            log.info("Step 1: Processing payment for order: {}", order.getOrderId());
            order.setStatus(Order.OrderStatus.PAYMENT_PROCESSING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            paymentServiceClient.processPayment(
                    order.getOrderId(),
                    order.getCustomerId(),
                    order.getTotalAmount()
            );
            log.info("Payment successful for order: {}", order.getOrderId());

            // Step 2: Reserve Inventory
            log.info("Step 2: Reserving inventory for order: {}", order.getOrderId());
            order.setStatus(Order.OrderStatus.INVENTORY_RESERVED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            inventoryServiceClient.reserveInventory(
                    order.getOrderId(),
                    order.getProductId(),
                    order.getQuantity()
            );
            log.info("Inventory reserved for order: {}", order.getOrderId());

            // Step 3: Send Notification
            log.info("Step 3: Sending notification for order: {}", order.getOrderId());
            notificationServiceClient.sendNotification(
                    order.getOrderId(),
                    order.getCustomerId(),
                    "Your order has been confirmed"
            );

            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order completed successfully: {}", order.getOrderId());

        } catch (CircuitBreakerOpenException ex) {
            log.error("Circuit breaker open for service: {}", ex.getServiceName());
            throw ex;
        } catch (ServiceException ex) {
            log.error("Service error during order processing: {}", ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .collect(Collectors.toMap(Order::getOrderId, order -> order));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ServiceException("OrderService", "INVALID_ORDER_INPUT", 
                "Total amount must be positive");
        }
        
        if (request.getQuantity() <= 0) {
            throw new ServiceException("OrderService", "INVALID_ORDER_INPUT", 
                "Quantity must be positive");
        }
        
        if (request.getTotalAmount().compareTo(new java.math.BigDecimal("1000000")) > 0) {
            throw new ServiceException("OrderService", "INVALID_ORDER_INPUT", 
                "Total amount exceeds maximum limit of 1,000,000");
        }
    }
}
