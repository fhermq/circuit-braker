package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @JsonProperty("customer_id")
    @NotBlank(message = "Customer ID is required")
    @Size(min = 3, max = 50, message = "Customer ID must be 3-50 characters")
    private String customerId;

    @JsonProperty("product_id")
    @NotBlank(message = "Product ID is required")
    @Size(min = 3, max = 50, message = "Product ID must be 3-50 characters")
    private String productId;

    @JsonProperty("quantity")
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Max(value = 1000, message = "Quantity cannot exceed 1000")
    private Integer quantity;

    @JsonProperty("total_amount")
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    @DecimalMax(value = "999999.99", message = "Total amount cannot exceed 999999.99")
    private BigDecimal totalAmount;
}
