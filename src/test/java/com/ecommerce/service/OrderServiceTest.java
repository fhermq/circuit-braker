package com.ecommerce.service;

import com.ecommerce.client.InventoryServiceClient;
import com.ecommerce.client.NotificationServiceClient;
import com.ecommerce.client.PaymentServiceClient;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.ServiceException;
import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest validOrderRequest;

    @BeforeEach
    void setUp() {
        validOrderRequest = OrderRequest.builder()
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Arrange
        when(paymentServiceClient.processPayment(any(), any(), any())).thenReturn(true);
        when(inventoryServiceClient.reserveInventory(any(), any(), any())).thenReturn(true);
        when(notificationServiceClient.sendNotification(any(), any(), any())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order order = orderService.createOrder(validOrderRequest);

        // Assert
        assertThat(order).isNotNull();
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("CUST-001");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("100.00"));

        verify(orderRepository, atLeast(4)).save(any(Order.class));
        verify(paymentServiceClient).processPayment(any(), any(), any());
        verify(inventoryServiceClient).reserveInventory(any(), any(), any());
        verify(notificationServiceClient).sendNotification(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("-100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Total amount must be positive");
    }

    @Test
    void shouldThrowExceptionForZeroQuantity() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(0)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void shouldThrowExceptionForAmountExceedingLimit() {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("2000000.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("exceeds maximum limit");
    }

    @Test
    void shouldRetrieveOrderById() {
        // Arrange
        Order mockOrder = Order.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("100.00"))
                .status(Order.OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findByOrderId("ORDER-001")).thenReturn(Optional.of(mockOrder));

        // Act
        Order retrievedOrder = orderService.getOrder("ORDER-001");

        // Assert
        assertThat(retrievedOrder).isNotNull();
        assertThat(retrievedOrder.getOrderId()).isEqualTo("ORDER-001");
        assertThat(retrievedOrder.getCustomerId()).isEqualTo("CUST-001");
        verify(orderRepository).findByOrderId("ORDER-001");
    }

    @Test
    void shouldReturnNullForNonExistentOrder() {
        // Arrange
        when(orderRepository.findByOrderId("NON-EXISTENT-ID")).thenReturn(Optional.empty());

        // Act
        Order order = orderService.getOrder("NON-EXISTENT-ID");

        // Assert
        assertThat(order).isNull();
        verify(orderRepository).findByOrderId("NON-EXISTENT-ID");
    }
}
