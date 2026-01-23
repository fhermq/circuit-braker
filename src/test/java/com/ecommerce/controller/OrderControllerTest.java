package com.ecommerce.controller;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderRequest validOrderRequest;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        validOrderRequest = OrderRequest.builder()
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        mockOrder = Order.builder()
                .orderId("ORDER-001")
                .customerId("CUST-001")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("100.00"))
                .status(Order.OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Arrange
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(mockOrder);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value("ORDER-001"))
                .andExpect(jsonPath("$.customer_id").value("CUST-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturnBadRequestForInvalidInput() throws Exception {
        // Arrange
        OrderRequest invalidRequest = OrderRequest.builder()
                .customerId("")
                .productId("PROD-001")
                .quantity(2)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        // Arrange
        when(orderService.getOrder("ORDER-001")).thenReturn(mockOrder);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/ORDER-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value("ORDER-001"))
                .andExpect(jsonPath("$.customer_id").value("CUST-001"));
    }

    @Test
    void shouldReturnAllOrders() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
