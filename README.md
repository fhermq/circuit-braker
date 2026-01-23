# Circuit Breaker Pattern Demo

A hands-on reference implementation demonstrating the Circuit Breaker pattern for resilience using Spring Boot, Resilience4j, and H2 database.

## 🎯 Project Overview

This project showcases how to build a resilient order processing system using the Circuit Breaker pattern. It serves as an educational reference for developers learning to handle failures gracefully in microservices architecture.

**Key Technologies:**
- Spring Boot 3.2
- Resilience4j 2.1
- H2 Database
- JPA/Hibernate
- Maven

**Status:** ✅ Reference Implementation (Educational)

## 🌟 Features

### Core Circuit Breaker Pattern
- 3 independent circuit breakers (Payment, Inventory, Notification)
- State management (CLOSED, OPEN, HALF_OPEN)
- Configurable thresholds and timeouts
- Automatic recovery testing
- Fallback mechanisms

### Production-Grade Components
- Custom exception hierarchy with error codes
- Global exception handler
- Multi-layer input validation
- Transaction management with @Transactional
- Strategic database indexes

### Database Design
- H2 in-memory/file-based database
- JPA/Hibernate ORM
- Proper data types (BigDecimal for money, LocalDateTime for timestamps)
- Automatic schema creation

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Installation

Clone the repository:
```bash
git clone <repository-url>
cd circuit-breaker
```

Build the project:
```bash
mvn clean install
```

Run the application:
```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

### Verify Installation

Check health:
```bash
curl http://localhost:8080/actuator/health
```

## 📚 API Documentation

### Create Order
**Endpoint:** `POST /api/v1/orders`

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "CUST001",
    "product_id": "PROD001",
    "quantity": 2,
    "total_amount": 99.99
  }'
```

**Response (201 Created):**
```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "CUST001",
  "product_id": "PROD001",
  "quantity": 2,
  "total_amount": 99.99,
  "status": "COMPLETED",
  "created_at": "2026-01-21T18:23:28",
  "updated_at": "2026-01-21T18:23:28"
}
```

### Get Order
**Endpoint:** `GET /api/v1/orders/{orderId}`

**Response:**
```json
{
  "order_id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "CUST001",
  "product_id": "PROD001",
  "quantity": 2,
  "total_amount": 99.99,
  "status": "COMPLETED",
  "created_at": "2026-01-21T18:23:28",
  "updated_at": "2026-01-21T18:23:28"
}
```

### Get All Orders
**Endpoint:** `GET /api/v1/orders`

### Error Response
```json
{
  "error_code": "CIRCUIT_BREAKER_OPEN",
  "message": "Circuit breaker is OPEN for service: PaymentService",
  "service": "PaymentService",
  "status": 503,
  "path": "/api/v1/orders",
  "timestamp": "2026-01-21T18:23:28"
}
```

## 🏗️ Project Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST Client                              │
│                    (curl, Postman, etc)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Order Controller (/api/v1/orders)                  │
│  POST   Create Order                                            │
│  GET    Get Order by ID                                         │
│  GET    Get All Orders                                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Order Service                                │
│  • Validate input (3 layers)                                    │
│  • Create order (PENDING)                                       │
│  • Orchestrate external services                                │
│  • Handle transactions (@Transactional)                         │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   ┌─────────┐    ┌──────────┐    ┌──────────────┐
   │ Payment │    │Inventory │    │Notification  │
   │ Service │    │ Service  │    │  Service     │
   │ Client  │    │ Client   │    │  Client      │
   └────┬────┘    └────┬─────┘    └──────┬───────┘
        │              │                 │
   [CB:CLOSED]    [CB:CLOSED]      [CB:CLOSED]
        │              │                 │
        └──────────────┴─────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │      H2 Database             │
        │  • Orders table              │
        │  • Indexes for performance   │
        │  • Persistent storage        │
        └──────────────────────────────┘
```

## 🔄 Request Flow - Happy Path

```
1. CLIENT REQUEST
   POST /api/v1/orders
   ↓
2. VALIDATION
   ├─ Bean Validation (format, constraints)
   ├─ Service Validation (business rules)
   └─ ✓ Valid
   ↓
3. ORDER CREATION
   ├─ Create Order entity
   ├─ Save to database (PENDING)
   └─ Status: PENDING
   ↓
4. PAYMENT PROCESSING
   ├─ Circuit Breaker: CLOSED
   ├─ Call PaymentServiceClient
   ├─ Payment succeeds
   └─ Status: PAYMENT_PROCESSING
   ↓
5. INVENTORY RESERVATION
   ├─ Circuit Breaker: CLOSED
   ├─ Call InventoryServiceClient
   ├─ Inventory reserved
   └─ Status: INVENTORY_RESERVED
   ↓
6. NOTIFICATION SENDING
   ├─ Circuit Breaker: CLOSED
   ├─ Call NotificationServiceClient
   ├─ Notification sent
   └─ Status: COMPLETED
   ↓
7. RESPONSE
   HTTP 201 Created
   {
     "order_id": "550e8400-e29b-41d4-a716-446655440000",
     "status": "COMPLETED"
   }
```

## ⚠️ Request Flow - Failure Scenario

```
1. CLIENT REQUEST
   POST /api/v1/orders
   (Payment service is failing)
   ↓
2. VALIDATION ✓
   ↓
3. ORDER CREATION
   Status: PENDING
   ↓
4. PAYMENT PROCESSING (Attempts 1-5)
   ├─ Circuit Breaker: CLOSED
   ├─ Call PaymentServiceClient
   ├─ Payment FAILS
   ├─ Failure count: 1, 2, 3, 4, 5
   ├─ Failure rate: 100% (5/5)
   └─ Threshold: 50% → CIRCUIT OPENS
   ↓
5. CIRCUIT BREAKER OPENS
   ├─ State: CLOSED → OPEN
   ├─ Reason: Failure rate exceeded
   └─ Wait: 10 seconds before recovery test
   ↓
6. NEXT REQUEST (While OPEN)
   ├─ Circuit Breaker: OPEN
   ├─ Immediate rejection (no call made)
   ├─ Throw CircuitBreakerOpenException
   └─ Status: FAILED
   ↓
7. RESPONSE
   HTTP 503 Service Unavailable
   {
     "error_code": "CIRCUIT_BREAKER_OPEN",
     "message": "Circuit breaker is OPEN for service: PaymentService",
     "status": 503
   }
   ↓
8. WAIT 10 SECONDS
   Circuit Breaker: OPEN → HALF_OPEN
   ↓
9. RECOVERY TEST (HALF_OPEN)
   ├─ Limited calls allowed: 3
   ├─ Call PaymentServiceClient
   ├─ Payment succeeds
   └─ Circuit Breaker: HALF_OPEN → CLOSED
   ↓
10. NORMAL OPERATION RESUMED
    Circuit Breaker: CLOSED
    Requests pass through
```

## 🔄 Circuit Breaker State Machine

```
                    ┌─────────────────────┐
                    │      CLOSED         │
                    │  (Normal Operation) │
                    │                     │
                    │ • Requests pass     │
                    │ • Failures tracked  │
                    │ • Failure rate < 50%│
                    └──────────┬──────────┘
                               │
                    Failure Rate > 50%
                    (5 failures in 10 calls)
                               │
                               ▼
                    ┌─────────────────────┐
                    │       OPEN          │
                    │  (Fail-Fast Mode)   │
                    │                     │
                    │ • Requests rejected │
                    │ • No calls made     │
                    │ • Fast failure      │
                    └──────────┬──────────┘
                               │
                    Wait 10 seconds
                    (waitDurationInOpenState)
                               │
                               ▼
                    ┌─────────────────────┐
                    │    HALF_OPEN        │
                    │  (Testing Recovery) │
                    │                     │
                    │ • Limited calls (3) │
                    │ • Testing service   │
                    │ • Monitoring result │
                    └──────────┬──────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
            Success (3 calls)      Failure (1 call)
                    │                     │
                    ▼                     ▼
              CLOSED ◄──────────────► OPEN
```

## 🔑 Key Concepts

### Circuit Breaker Pattern

A circuit breaker prevents cascading failures by monitoring for failures and temporarily blocking requests to a failing service.

**States:**
1. **CLOSED** - Normal operation, requests pass through
2. **OPEN** - Service failing, requests rejected immediately (fail-fast)
3. **HALF_OPEN** - Testing recovery, limited requests allowed

**Configuration:**
- Sliding Window Size: 10 calls
- Minimum Calls: 5 (before evaluating failure rate)
- Failure Rate Threshold: 50%
- Wait Duration: 5-10 seconds (before attempting recovery)
- Slow Call Threshold: 2 seconds

### Fallback Strategies

- **Payment Service**: Strict - fails the order
- **Inventory Service**: Strict - fails the order
- **Notification Service**: Lenient - logs and continues (non-critical)

## 🧪 Testing the Circuit Breaker

### Scenario 1: Normal Operation
```bash
# Create order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"CUST001","product_id":"PROD001","quantity":2,"total_amount":99.99}'

# Check status
curl http://localhost:8080/actuator/circuitbreakers
```

### Scenario 2: Service Degradation

Enable payment failure:
```bash
curl -X POST "http://localhost:8080/api/simulation/payment/fail?enable=true"
```

Create 5-6 orders (will fail):
```bash
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/v1/orders \
    -H "Content-Type: application/json" \
    -d "{\"customer_id\":\"CUST00$i\",\"product_id\":\"PROD001\",\"quantity\":1,\"total_amount\":50.00}"
done
```

Observe circuit breaker opens (503 responses). Wait 10 seconds, then disable failure:
```bash
curl -X POST "http://localhost:8080/api/simulation/payment/fail?enable=false"
```

Create order - circuit tests recovery and closes.

### Scenario 3: Cascading Failures

Enable all failures:
```bash
curl -X POST "http://localhost:8080/api/simulation/payment/fail?enable=true"
curl -X POST "http://localhost:8080/api/simulation/inventory/fail?enable=true"
curl -X POST "http://localhost:8080/api/simulation/notification/fail?enable=true"
```

Observe circuit breakers opening. Disable one by one and observe recovery.

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### Database (H2 Console)
```
http://localhost:8080/h2-console
```

Connection details:
- JDBC URL: `jdbc:h2:mem:orderdb`
- User: `sa`
- Password: (empty)

## 🐳 Docker Setup (H2 UI Only)

Start H2 UI container:
```bash
docker-compose up
```

Access H2 UI:
```
http://localhost:8081
```

Connection details:
- Host: `localhost`
- Port: `8082`
- Database: `/data/orderdb`

Stop containers:
```bash
docker-compose down
```

## 🧪 Running Tests

```bash
mvn clean test
```

Tests cover:
- Unit tests (OrderServiceTest - 6 tests)
- Integration tests (OrderControllerTest - 4 tests)
- Success scenarios
- Failure scenarios
- Validation

## 💡 Best Practices Demonstrated

1. **Error Handling** - Custom exception hierarchy with error codes
2. **Data Validation** - Multi-layer validation (Bean, Controller, Service)
3. **Transaction Management** - @Transactional for ACID properties
4. **API Design** - RESTful with versioning and DTOs
5. **Code Organization** - Clean separation of concerns

## 📖 Code Structure

```
src/main/java/com/ecommerce/
├── client/              # External service clients with circuit breakers
├── controller/          # REST endpoints
├── service/             # Business logic & orchestration
├── model/               # JPA entities
├── repository/          # Data access
├── dto/                 # Data transfer objects
├── exception/           # Custom exceptions & handlers
└── constant/            # Error codes
```

## 🎓 Interview Discussion Points

1. **Why Circuit Breaker?**
   - Prevents cascading failures
   - Provides fail-fast behavior
   - Enables graceful degradation
   - Allows automatic recovery

2. **When to Use?**
   - External API calls
   - Database connections
   - Microservice communication
   - Any operation that can fail

3. **Circuit Breaker vs Retry?**
   - Retry: Good for transient failures
   - Circuit Breaker: Good for persistent failures
   - Often used together

4. **Monitoring?**
   - Track state transitions
   - Alert on repeated failures
   - Monitor recovery time
   - Measure impact on user experience

5. **Configuration Tuning?**
   - Sliding window size affects sensitivity
   - Failure threshold determines when to open
   - Wait duration affects recovery speed
   - Slow call threshold prevents hanging requests

## 📚 Next Steps for Learning

- Implement retry logic with exponential backoff
- Add bulkhead pattern for thread isolation
- Implement distributed tracing
- Add rate limiting
- Implement Saga pattern for distributed transactions

## 🔗 Learning Resources

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Microservices Patterns](https://microservices.io/patterns/index.html)

## 📝 License

This project is provided as-is for educational purposes.
