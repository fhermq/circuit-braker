# Reusable Standards & Patterns from Saga Project

**Purpose:** Share proven patterns and standards from this project with other projects  
**Created:** January 20, 2026  
**Based On:** Sprint 1 Code Review & Implementation

---

## Overview

This document captures the best practices and patterns established in the Saga Pattern Demo project that can be reused in other Java/Spring Boot projects.

---

## 1. Exception Handling Pattern

### Problem
- Generic `RuntimeException` throughout codebase
- Inconsistent error handling
- Difficult to debug and handle errors on client side

### Solution: Custom Exception Hierarchy

```java
// Base exception with error code
public abstract class ApplicationException extends RuntimeException {
    private final String errorCode;
    
    public ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

// Specific exceptions
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceId) {
        super("RESOURCE_NOT_FOUND", 
            String.format("Resource not found: %s", resourceId));
    }
}

public class InvalidInputException extends ApplicationException {
    public InvalidInputException(String message) {
        super("INVALID_INPUT", message);
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.NOT_FOUND.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            InvalidInputException ex, WebRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### Benefits
- ✅ Specific exception types for different scenarios
- ✅ Error codes for client-side handling
- ✅ Consistent error responses
- ✅ Centralized error handling
- ✅ Proper HTTP status codes

### Reuse Checklist
- [ ] Create base exception class with error code
- [ ] Create specific exception classes
- [ ] Implement GlobalExceptionHandler
- [ ] Create ErrorResponse DTO
- [ ] Add error code constants
- [ ] Document error codes

---

## 2. API Design Pattern

### Problem
- Inconsistent API design
- Query parameters for complex data
- No versioning strategy
- Mixing entities with API responses

### Solution: RESTful API with DTOs

```java
// API versioning
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    
    // Request DTO with validation
    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @RequestBody @Valid CreateResourceRequest request) {
        
        Resource resource = service.create(
            request.getName(), 
            request.getDescription()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResourceResponse.from(resource));
    }
    
    // Response DTO
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(@PathVariable String id) {
        Resource resource = service.getById(id);
        return ResponseEntity.ok(ResourceResponse.from(resource));
    }
}

// Request DTO
@Data
@Builder
public class CreateResourceRequest {
    @JsonProperty("name")
    @NotBlank(message = "Name is required")
    private String name;
    
    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    private String description;
}

// Response DTO
@Data
@Builder
public class ResourceResponse {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    public static ResourceResponse from(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
```

### Benefits
- ✅ API versioning for backward compatibility
- ✅ JSON request/response format
- ✅ Separation of concerns (entity vs DTO)
- ✅ Input validation at API layer
- ✅ Consistent response format

### Reuse Checklist
- [ ] Use `/api/v1/` prefix for versioning
- [ ] Create separate request/response DTOs
- [ ] Use `@JsonProperty` for JSON mapping
- [ ] Apply `@Valid` annotation
- [ ] Include factory methods in DTOs
- [ ] Document API endpoints

---

## 3. Data Type Standards

### Problem
- Using `Double` for monetary values (rounding errors)
- Inconsistent timestamp handling
- No precision specification for decimals

### Solution: Proper Data Types

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    
    // Monetary values: BigDecimal with precision
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    
    // Timestamps: LocalDateTime with lifecycle methods
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Usage Examples

```java
// ✅ Correct: BigDecimal for money
BigDecimal amount = new BigDecimal("100.00");
BigDecimal total = amount.add(new BigDecimal("50.00"));

// ❌ Wrong: Double for money
Double amount = 100.0;
Double total = amount + 50.0; // Rounding errors!

// ✅ Correct: LocalDateTime for timestamps
LocalDateTime created = LocalDateTime.now();

// ❌ Wrong: Date for timestamps
Date created = new Date();
```

### Benefits
- ✅ Accurate financial calculations
- ✅ No rounding errors
- ✅ Consistent timestamp handling
- ✅ Database precision specification
- ✅ Automatic timestamp management

### Reuse Checklist
- [ ] Use `BigDecimal` for monetary values
- [ ] Specify `precision=19, scale=2` for decimals
- [ ] Use `LocalDateTime` for timestamps
- [ ] Add `@PrePersist` and `@PreUpdate` methods
- [ ] Never use `Double` or `Float` for money
- [ ] Document data type choices

---

## 4. Database Optimization Pattern

### Problem
- No indexes on frequently queried columns
- Slow queries at scale
- No optimization strategy

### Solution: Strategic Indexing

```java
@Entity
@Table(name = "orders", indexes = {
    // Single column indexes
    @Index(name = "idx_order_id", columnList = "order_id", unique = true),
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    
    // Composite indexes for common filters
    @Index(name = "idx_customer_status", 
            columnList = "customer_id,status"),
    @Index(name = "idx_status_created", 
            columnList = "status,created_at")
})
public class Order {
    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Indexing Strategy
1. **Unique Identifiers:** Always index
2. **Foreign Keys:** Always index
3. **Filter Columns:** Index if frequently filtered
4. **Sort Columns:** Index if frequently sorted
5. **Composite Indexes:** For common filter combinations

### Benefits
- ✅ 30-50% faster queries
- ✅ Better scalability
- ✅ Reduced database load
- ✅ Improved user experience

### Reuse Checklist
- [ ] Identify frequently queried columns
- [ ] Create indexes for filter columns
- [ ] Create composite indexes for common combinations
- [ ] Monitor query performance
- [ ] Adjust indexes based on usage patterns
- [ ] Document indexing strategy

---

## 5. Input Validation Pattern

### Problem
- No input validation
- Invalid data propagation
- Inconsistent error messages

### Solution: Multi-Layer Validation

```java
// Layer 1: Bean Validation (Declarative)
@Data
@Builder
public class CreateOrderRequest {
    @JsonProperty("customer_id")
    @NotBlank(message = "Customer ID is required")
    @Size(min = 3, max = 50, message = "Customer ID must be 3-50 characters")
    private String customerId;
    
    @JsonProperty("amount")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
}

// Layer 2: Controller Validation
@PostMapping
public ResponseEntity<OrderResponse> create(
        @RequestBody @Valid CreateOrderRequest request) {
    // Bean Validation already applied
    Order order = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(order));
}

// Layer 3: Service Validation (Business Rules)
@Service
public class OrderService {
    
    public Order create(String customerId, BigDecimal amount) {
        validateOrderInput(customerId, amount);
        // Create order
    }
    
    private void validateOrderInput(String customerId, BigDecimal amount) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidInputException("Customer ID cannot be empty");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount must be positive");
        }
        
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new InvalidInputException("Amount exceeds maximum limit");
        }
    }
}
```

### Validation Layers
1. **Bean Validation:** Format and constraint validation
2. **Controller:** Request structure validation
3. **Service:** Business rule validation

### Benefits
- ✅ Comprehensive input validation
- ✅ Clear error messages
- ✅ Business rule enforcement
- ✅ Consistent validation approach

### Reuse Checklist
- [ ] Add Bean Validation annotations to DTOs
- [ ] Use `@Valid` in controllers
- [ ] Implement service-level validation
- [ ] Create custom validation annotations if needed
- [ ] Document validation rules
- [ ] Test validation scenarios

---

## 6. Logging Best Practices

### Problem
- Insufficient logging context
- String concatenation in logs
- Inconsistent log levels

### Solution: Structured Logging

```java
@Slf4j
@Service
public class OrderService {
    
    public Order create(String customerId, BigDecimal amount) {
        // Use parameterized messages (not concatenation)
        log.info("Creating order for customer: {} with amount: {}", 
            customerId, amount);
        
        try {
            Order order = new Order();
            order.setCustomerId(customerId);
            order.setAmount(amount);
            
            Order saved = repository.save(order);
            
            log.info("Order created successfully: orderId={}, customerId={}", 
                saved.getId(), customerId);
            
            return saved;
        } catch (Exception e) {
            log.error("Failed to create order for customer: {}", 
                customerId, e);
            throw new SagaException("Order creation failed", e);
        }
    }
}
```

### Log Levels
- **INFO:** Normal operations (order created, payment processed)
- **WARN:** Unexpected but recoverable (order not found, retry)
- **ERROR:** Failures requiring attention (payment failed, exception)
- **DEBUG:** Detailed debugging information

### Benefits
- ✅ Better debugging capability
- ✅ Easier troubleshooting
- ✅ Performance (no string concatenation)
- ✅ Consistent logging format

### Reuse Checklist
- [ ] Use `@Slf4j` annotation
- [ ] Use parameterized messages
- [ ] Include relevant context
- [ ] Use appropriate log levels
- [ ] Log at method entry/exit
- [ ] Log exceptions with stack trace

---

## 7. Testing Pattern

### Problem
- Insufficient test coverage
- No integration testing
- Tests not isolated

### Solution: Comprehensive Testing

```java
// Unit Tests
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository repository;
    
    @InjectMocks
    private OrderService service;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal amount = new BigDecimal("100.00");
        
        // Act
        Order order = service.create(customerId, amount);
        
        // Assert
        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        verify(repository).save(any(Order.class));
    }
    
    @Test
    void shouldThrowExceptionForInvalidAmount() {
        assertThrows(InvalidInputException.class,
            () -> service.create("CUST-001", new BigDecimal("-100")));
    }
}

// Integration Tests
@SpringBootTest
@EmbeddedKafka
class OrderIntegrationTest {
    
    @Autowired
    private OrderService service;
    
    @Autowired
    private OrderRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
    
    @Test
    void shouldCompleteOrderFlow() {
        // Test complete workflow
        Order order = service.create("CUST-001", new BigDecimal("100.00"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

### Testing Strategy
1. **Unit Tests:** Test services in isolation
2. **Integration Tests:** Test with real dependencies
3. **Test Data Cleanup:** Use `@BeforeEach`
4. **Both Success & Failure:** Test happy path and errors

### Benefits
- ✅ High test coverage
- ✅ Regression prevention
- ✅ Confidence in changes
- ✅ Documentation through tests

### Reuse Checklist
- [ ] Create unit tests for services
- [ ] Create integration tests for workflows
- [ ] Use `@BeforeEach` for cleanup
- [ ] Test both success and failure scenarios
- [ ] Aim for >80% code coverage
- [ ] Use meaningful test names

---

## Implementation Checklist

Use this checklist when implementing these patterns in a new project:

### Exception Handling
- [ ] Create base exception class
- [ ] Create specific exception classes
- [ ] Implement GlobalExceptionHandler
- [ ] Create ErrorResponse DTO
- [ ] Add error code constants
- [ ] Document error codes

### API Design
- [ ] Use `/api/v1/` versioning
- [ ] Create request/response DTOs
- [ ] Apply Bean Validation
- [ ] Use `@Valid` annotation
- [ ] Include factory methods
- [ ] Document endpoints

### Data Types
- [ ] Use BigDecimal for money
- [ ] Use LocalDateTime for timestamps
- [ ] Add lifecycle methods
- [ ] Specify database precision
- [ ] Never use Double for money

### Database
- [ ] Identify query patterns
- [ ] Create single-column indexes
- [ ] Create composite indexes
- [ ] Monitor performance
- [ ] Document strategy

### Validation
- [ ] Add Bean Validation annotations
- [ ] Implement service validation
- [ ] Create custom validators if needed
- [ ] Test validation scenarios
- [ ] Document rules

### Logging
- [ ] Use `@Slf4j` annotation
- [ ] Use parameterized messages
- [ ] Include context
- [ ] Use appropriate levels
- [ ] Log exceptions

### Testing
- [ ] Create unit tests
- [ ] Create integration tests
- [ ] Clean up test data
- [ ] Test success and failure
- [ ] Aim for high coverage

---

## Metrics to Track

### Code Quality
- Compilation warnings: 0
- Test pass rate: 100%
- Code duplication: <5%
- JavaDoc coverage: >90%

### Performance
- Database query time: <100ms
- API response time: <200ms
- Build time: <5 minutes

### Maintainability
- Cyclomatic complexity: <10
- Method length: <30 lines
- Class cohesion: High
- Coupling: Low

---

## References

- [Saga Project Sprint 1 Review](./sprints/SPRINT_1_REVIEW.md)
- [Project Guidelines](./.kiro/steering/project-guidelines.md)
- [Spring Boot Best Practices](https://spring.io/guides)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)

---

**Last Updated:** January 20, 2026  
**Version:** 1.0  
**Status:** Ready for use in other projects
