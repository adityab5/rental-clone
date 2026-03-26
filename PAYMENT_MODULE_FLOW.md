# Payment Module - End-to-End Flow Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Module Components](#module-components)
4. [Data Models](#data-models)
5. [Payment Flow](#payment-flow)
6. [API Endpoints](#api-endpoints)
7. [Error Handling](#error-handling)
8. [Security & Validation](#security--validation)
9. [Configuration](#configuration)
10. [Database Schema](#database-schema)

---

## Overview

The Payment Module is a comprehensive payment processing system built into the Rental Local Spring Boot application. It integrates with **Razorpay** as the primary payment gateway to handle order creation and payment verification for equipment rental transactions.

### Key Features
- Create payment orders for rentals
- Verify payments with cryptographic signatures
- Track payment status (PENDING, SUCCESS, FAILED)
- Audit logging of all payment transactions
- HMAC-SHA256 signature verification for security

---

## Architecture

The payment module follows a **layered architecture pattern**:

```
┌─────────────────────────────────────────────────────────────┐
│                      REST API Layer                         │
│                   (PaymentController)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                  Service Layer                              │
│                 (PaymentService)                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
    ┌───▼──┐   ┌──────▼─────┐   ┌───▼──────────┐
    │      │   │            │   │              │
    │      │   │   Payment  │   │              │
    │      │   │ Repository │   │              │
    │      │   │            │   │              │
    │      │   └──────▲─────┘   │              │
    │      │          │         │              │
    │      │          │         │              │
    │ ────────────────┼─────────────────────────│─► Database
    │      │          │         │              │
    │      │          │         │              │
    │      │   ┌──────▼─────┐   │              │
    │      │   │  Rental &  │   │              │
    │      │   │  User Data │   │              │
    │      │   │ Repositories  │              │
    │      │   └────────────┘   │              │
    │      │                    │              │
    │      │                    └──────────────┘
    │      │
    │  Utilities         Gateway                  Config
    │  - HMAC-SHA256  - RazorpayGateway  - RazorpayProperties
    │                   ClientImpl
    │
    │
    └──────────────────────────────────────────►Razorpay API
```

---

## Module Components

### 1. **PaymentController** 
**Location**: `controller/PaymentController.java`

HTTP REST endpoint handler for payment operations.

**Endpoints**:
- `POST /api/payments/create-order` - Create a payment order
- `POST /api/payments/verify` - Verify a payment

**Responsibilities**:
- Receive and validate HTTP requests
- Log payment operations
- Delegate to PaymentService for business logic
- Return formatted responses

---

### 2. **PaymentService**
**Location**: `service/PaymentService.java`

Core business logic for payment operations.

**Key Methods**:

#### `createOrder(CreatePaymentOrderRequest request)`
- Validates rental exists
- Checks rental amount is valid
- Creates or retrieves payment entity
- Prevents duplicate successful payments
- Calls Razorpay gateway to create order
- Persists payment with PENDING status
- Returns order details to client

#### `verifyPayment(VerifyPaymentRequest request)`
- Retrieves payment by rental ID
- Validates order ID matches persisted value
- Reconstructs HMAC-SHA256 signature payload
- Verifies signature using secret key
- On success: Updates status to SUCCESS, sets payment timestamp
- On failure: Updates status to FAILED, logs for audit
- Returns verification result

---

### 3. **Payment Entity**
**Location**: `entity/Payment.java`

JPA entity representing a payment record in the database.

**Fields**:
```
- id: Long (Primary Key, Auto-generated)
- rental: Rental (One-to-One relationship, unique)
- user: User (Many-to-One relationship)
- amount: BigDecimal (Payment amount in rupees)
- status: PaymentStatus (PENDING, SUCCESS, FAILED)
- gatewayRef: String (Razorpay Order ID)
- gatewayPaymentId: String (Razorpay Payment ID)
- gatewaySignature: String (Razorpay signature for verification)
- paidAt: LocalDateTime (Timestamp when payment succeeded)
- createdAt: LocalDateTime (Inherited from BaseEntity)
- updatedAt: LocalDateTime (Inherited from BaseEntity)
```

**Key Relationships**:
- **One-to-One with Rental**: Each rental can have one payment (unique constraint on rental_id)
- **Many-to-One with User**: Each user can have multiple payments

---

### 4. **DTOs (Data Transfer Objects)**

#### CreatePaymentOrderRequest
```java
public record CreatePaymentOrderRequest(
    @NotNull Long rentalId
) { }
```
Input for initiating payment order creation.

#### CreatePaymentOrderResponse
```java
public record CreatePaymentOrderResponse(
    Long paymentId,
    Long rentalId,
    String razorpayOrderId,
    BigDecimal amount,
    String currency,
    String status
) { }
```
Response containing Razorpay order details for client-side payment processing.

#### VerifyPaymentRequest
```java
public record VerifyPaymentRequest(
    @NotNull Long rentalId,
    @NotBlank String razorpayOrderId,
    @NotBlank String razorpayPaymentId,
    @NotBlank String razorpaySignature
) { }
```
Input containing Razorpay payment verification details from client.

#### VerifyPaymentResponse
```java
public record VerifyPaymentResponse(
    Long paymentId,
    Long rentalId,
    String razorpayOrderId,
    String razorpayPaymentId,
    PaymentStatus status,
    boolean verified
) { }
```
Response confirming payment verification result.

---

### 5. **PaymentRepository**
**Location**: `repository/PaymentRepository.java`

Spring Data JPA repository for database operations.

**Methods**:
- `findByRentalId(Long rentalId)` - Get payment by rental (unique)
- `findAllByUserIdAndStatus(Long userId, PaymentStatus status)` - Get user's payments by status
- `findByGatewayRef(String gatewayRef)` - Get payment by Razorpay order ID
- `countByStatus(PaymentStatus status)` - Count payments by status
- `sumAmountByStatus(PaymentStatus status)` - Calculate total amount by status

---

### 6. **RazorpayGatewayClient & Implementation**
**Location**: `service/gateway/RazorpayGatewayClient.java`, `RazorpayGatewayClientImpl.java`

Interface and implementation for Razorpay API integration.

**Key Responsibilities**:
- Create orders with Razorpay API
- Handle currency conversion (rupees to paise)
- Generate unique receipts for tracking
- Validate credentials before API calls
- Convert Razorpay responses to internal format

**Process Flow**:
1. Validate amount > 0
2. Validate Razorpay credentials (key-id, key-secret)
3. Convert amount from rupees to paise (multiply by 100)
4. Generate unique receipt: `rental_{rentalId}_{timestamp}`
5. Create JSON request with amount, currency, receipt
6. Call Razorpay API with credentials
7. Parse order response (ID, currency, amount)
8. Return RazorpayOrderResult

---

### 7. **HMAC-SHA256 Utility**
**Location**: `util/HmacSHA256Util.java`

Cryptographic utility for signature generation and verification.

**Methods**:

#### `generateSignature(String data, String secret)`
- Creates HMAC-SHA256 hash of payload
- Uses secret key from configuration
- Returns hexadecimal encoded signature
- Validates inputs (non-blank)

#### `isValidSignature(String payload, String providedSignature, String secret)`
- Generates expected signature from payload
- Compares with provided signature (case-insensitive)
- Uses constant-time comparison (MessageDigest.isEqual)
- Returns true only if signatures match

**Security Notes**:
- Constant-time comparison prevents timing attacks
- Hexadecimal encoding for signature representation
- UTF-8 character encoding for consistency

---

### 8. **PaymentStatus Enum**
**Location**: `enums/PaymentStatus.java`

```java
public enum PaymentStatus {
    PENDING,    // Order created, awaiting payment
    SUCCESS,    // Payment verified and confirmed
    FAILED      // Payment verification failed
}
```

---

### 9. **RazorpayProperties**
**Location**: `config/RazorpayProperties.java`

Configuration class for Razorpay credentials.

**Properties**:
```
razorpay.key-id: String
razorpay.key-secret: String
razorpay.currency: String (default: "INR")
```

**Configuration Source**: `application-dev.properties`

---

### 10. **Exception Classes**

#### PaymentNotFoundException
- Thrown when payment or rental not found
- HTTP Status: 404 Not Found

#### PaymentOperationException
- Thrown for validation/operation failures
- HTTP Status: 400 Bad Request
- Examples: Invalid amount, signature generation failure

#### PaymentGatewayException
- Thrown for Razorpay API failures
- HTTP Status: 503 Service Unavailable
- Examples: Missing credentials, API connection error

#### InvalidPaymentSignatureException
- Thrown when signature verification fails
- HTTP Status: 401 Unauthorized
- Indicates potential tampering or replay attack

---

## Payment Flow

### **Step 1: Order Creation Flow**

```
┌─────────────────────────────────────────────────────────────────┐
│ Client initiates payment by sending rental ID                   │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │ POST /api/payments/create-order │
        │ {                              │
        │   "rentalId": 42               │
        │ }                              │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ PaymentController.createOrder()│
        │ - Validates request            │
        │ - Logs operation               │
        └────────────┬───────────────────┘
                     │
                     ▼
        ┌────────────────────────────────┐
        │ PaymentService.createOrder()   │
        │                                │
        │ 1. Find Rental by ID           │
        │    └─ If not found:            │
        │       throw PaymentNotFound    │
        │                                │
        │ 2. Validate rental.totalCost   │
        │    └─ If <= 0:                 │
        │       throw PaymentOperation   │
        │                                │
        │ 3. Find Payment by rental ID   │
        │    └─ If exists:               │
        │       Check if already SUCCESS │
        │       If SUCCESS:              │
        │         throw PaymentOperation │
        │                                │
        │ 4. Create Payment entity       │
        │    └─ New or existing (for     │
        │       re-attempt handling)     │
        │                                │
        │ 5. Call Razorpay Gateway       │
        │    └─ RazorpayGatewayClientImpl│
        │       .createOrder(rental.id, │
        │                 totalCost,    │
        │                 currency)     │
        └────────────┬───────────────────┘
                     │
                     ▼
    ┌────────────────────────────────────┐
    │ RazorpayGatewayClientImpl.createOrder│
    │                                     │
    │ 1. Validate amount > 0              │
    │                                     │
    │ 2. Validate credentials not blank   │
    │    └─ key-id, key-secret            │
    │                                     │
    │ 3. Convert rupees to paise:         │
    │    amount * 100                     │
    │                                     │
    │ 4. Generate receipt:                │
    │    "rental_" + rentalId +           │
    │    "_" + currentTimestamp           │
    │                                     │
    │ 5. Create RazorpayClient with       │
    │    credentials                      │
    │                                     │
    │ 6. Call Razorpay API:               │
    │    razorpayClient.orders.create()   │
    │                                     │
    │ 7. Extract from response:           │
    │    - orderId                        │
    │    - currency                       │
    │    - amountInPaise                  │
    │                                     │
    │ 8. Return RazorpayOrderResult       │
    │    └─ If API fails:                 │
    │       throw PaymentGatewayException │
    └────────────┬────────────────────────┘
                 │
                 ▼ (Back to PaymentService)
    ┌──────────────────────────────────────┐
    │ Update Payment entity:               │
    │                                      │
    │ - Set amount = rental.totalCost      │
    │ - Set status = PENDING               │
    │ - Set gatewayRef = orderId           │
    │ - Clear gatewayPaymentId             │
    │ - Clear gatewaySignature             │
    │ - Clear paidAt                       │
    │                                      │
    │ Save to database                     │
    └────────────┬─────────────────────────┘
                 │
                 ▼
    ┌──────────────────────────────────────┐
    │ Return CreatePaymentOrderResponse:   │
    │ {                                    │
    │   "paymentId": 1,                    │
    │   "rentalId": 42,                    │
    │   "razorpayOrderId": "order_xxx",    │
    │   "amount": 5000.00,                 │
    │   "currency": "INR",                 │
    │   "status": "PENDING"                │
    │ }                                    │
    └────────────┬─────────────────────────┘
                 │
                 ▼
    ┌──────────────────────────────────────┐
    │ Client receives order details        │
    │ Displays Razorpay payment form       │
    │ User completes payment on Razorpay   │
    └──────────────────────────────────────┘
```

---

### **Step 2: Payment Verification Flow**

```
┌──────────────────────────────────────────────────────────────┐
│ Client receives payment confirmation from Razorpay           │
│ Extracts: orderId, paymentId, signature                      │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
        ┌──────────────────────────────────────┐
        │ POST /api/payments/verify             │
        │ {                                    │
        │   "rentalId": 42,                    │
        │   "razorpayOrderId": "order_xxx",    │
        │   "razorpayPaymentId": "pay_yyy",    │
        │   "razorpaySignature": "sig_zzz"     │
        │ }                                    │
        └────────────┬─────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────┐
        │ PaymentController.verifyPayment()  │
        │ - Validates request                │
        │ - Logs operation                   │
        └────────────┬───────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────┐
        │ PaymentService.verifyPayment()     │
        │                                    │
        │ 1. Find Payment by rental ID       │
        │    └─ If not found:                │
        │       throw PaymentNotFound        │
        │                                    │
        │ 2. Normalize inputs (trim):        │
        │    - razorpayOrderId               │
        │    - razorpayPaymentId             │
        │    - razorpaySignature             │
        │                                    │
        │ 3. Get persisted gatewayRef        │
        │    └─ If missing/blank:            │
        │       throw PaymentOperation       │
        │                                    │
        │ 4. Validate OrderId match:         │
        │    persisted == requested          │
        │    └─ If not equal:                │
        │       throw PaymentOperation       │
        │                                    │
        │ 5. Build signature payload:        │
        │    payload = orderId + "|" +       │
        │              paymentId             │
        │    Example: "order_xxx|pay_yyy"    │
        └────────────┬───────────────────────┘
                     │
                     ▼
        ┌────────────────────────────────────┐
        │ HmacSHA256Util.isValidSignature()  │
        │                                    │
        │ 1. Check signature not blank       │
        │                                    │
        │ 2. Generate expected signature:    │
        │    - HMAC-SHA256(payload, secret)  │
        │    - Convert to hexadecimal        │
        │                                    │
        │ 3. Compare signatures              │
        │    - Normalize: trim, lowercase    │
        │    - Use constant-time comparison  │
        │    - Prevents timing attacks       │
        │                                    │
        │ 4. Return true/false               │
        └────────────┬───────────────────────┘
                     │
    ┌────────────────┴────────────────┐
    │                                 │
    ▼                                 ▼
SIGNATURE VALID                  SIGNATURE INVALID
    │                                 │
    ▼                                 ▼
┌──────────────────┐          ┌──────────────────┐
│ Update Payment:  │          │ Update Payment:  │
│                  │          │                  │
│ - status =       │          │ - status = FAILED│
│   SUCCESS        │          │ - paidAt = null  │
│ - paidAt = now   │          │                  │
│ - gatewayPayment │          │ Save to DB       │
│   Id = provided  │          │                  │
│ - gateway        │          │ Log warning:     │
│   Signature =    │          │ Signature        │
│   provided       │          │ verification     │
│                  │          │ failed           │
│ Save to DB       │          │                  │
│                  │          │ Throw:           │
│ Log info:        │          │ InvalidPayment   │
│ Signature        │          │ SignatureExcept. │
│ verified OK      │          │                  │
└────────┬─────────┘          └──────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ Return VerifyPaymentResponse:        │
│ {                                    │
│   "paymentId": 1,                    │
│   "rentalId": 42,                    │
│   "razorpayOrderId": "order_xxx",    │
│   "razorpayPaymentId": "pay_yyy",    │
│   "status": "SUCCESS",               │
│   "verified": true                   │
│ }                                    │
└────────────┬──────────────────────────┘
             │
             ▼
┌──────────────────────────────────────┐
│ Client receives verification result  │
│ Rental is now paid and confirmed     │
└──────────────────────────────────────┘
```

---

## API Endpoints

### 1. Create Payment Order

**Endpoint**: `POST /api/payments/create-order`

**Request Body**:
```json
{
  "rentalId": 42
}
```

**Success Response** (HTTP 201 Created):
```json
{
  "paymentId": 1,
  "rentalId": 42,
  "razorpayOrderId": "order_1A2B3C4D5E",
  "amount": 5000.00,
  "currency": "INR",
  "status": "PENDING"
}
```

**Error Responses**:

| Status | Exception | Message |
|--------|-----------|---------|
| 404 | PaymentNotFoundException | Rental not found for id: {id} |
| 400 | PaymentOperationException | Rental amount is invalid for payment |
| 400 | PaymentOperationException | Payment already marked successful for rental id: {id} |
| 503 | PaymentGatewayException | Failed to create Razorpay order |
| 503 | PaymentGatewayException | Razorpay credentials are not configured |

---

### 2. Verify Payment

**Endpoint**: `POST /api/payments/verify`

**Request Body**:
```json
{
  "rentalId": 42,
  "razorpayOrderId": "order_1A2B3C4D5E",
  "razorpayPaymentId": "pay_1F2G3H4I5J",
  "razorpaySignature": "abc123def456xyz789..."
}
```

**Success Response** (HTTP 200 OK):
```json
{
  "paymentId": 1,
  "rentalId": 42,
  "razorpayOrderId": "order_1A2B3C4D5E",
  "razorpayPaymentId": "pay_1F2G3H4I5J",
  "status": "SUCCESS",
  "verified": true
}
```

**Error Responses**:

| Status | Exception | Message |
|--------|-----------|---------|
| 404 | PaymentNotFoundException | No payment initiated for rental id: {id} |
| 400 | PaymentOperationException | Persisted order id is missing for rental id: {id} |
| 400 | PaymentOperationException | Order id mismatch for rental id: {id} |
| 401 | InvalidPaymentSignatureException | Invalid Razorpay signature |

---

## Error Handling

### Exception Hierarchy

```
RuntimeException
├── PaymentNotFoundException (404 Not Found)
├── PaymentOperationException (400 Bad Request)
├── PaymentGatewayException (503 Service Unavailable)
└── InvalidPaymentSignatureException (401 Unauthorized)
```

### Common Error Scenarios

#### 1. Rental Not Found
**Cause**: Invalid rentalId in request
**Handling**: Return 404 with message "Rental not found for id: {id}"
**Action**: Client should verify rental exists before initiating payment

#### 2. Invalid Amount
**Cause**: Rental.totalCost is null or <= 0
**Handling**: Return 400 with message "Rental amount is invalid for payment"
**Action**: Ensure rental cost is calculated correctly

#### 3. Duplicate Payment
**Cause**: Attempting to pay for a rental that already has SUCCESS payment
**Handling**: Return 400 with message "Payment already marked successful for rental id: {id}"
**Action**: Payment already processed; no retry needed

#### 4. Order ID Mismatch
**Cause**: Client provides different orderId than persisted one
**Handling**: Return 400 with message "Order id mismatch for rental id: {id}"
**Action**: Potential tampering; customer should re-initiate payment

#### 5. Signature Verification Failed
**Cause**: Invalid HMAC-SHA256 signature from Razorpay
**Handling**: 
- Update payment status to FAILED
- Log warning with details
- Return 401 with message "Invalid Razorpay signature"
**Action**: Potential security issue; reject payment and retry

#### 6. Gateway Credentials Missing
**Cause**: Razorpay key-id or key-secret not configured
**Handling**: Return 503 with message "Razorpay credentials are not configured"
**Action**: System administrator must configure credentials

#### 7. Razorpay API Failure
**Cause**: Network error or Razorpay service down
**Handling**: Return 503 with message "Failed to create Razorpay order"
**Action**: Retry operation after service recovery

---

## Security & Validation

### Input Validation

#### CreatePaymentOrderRequest
- `rentalId`: @NotNull (must be provided)
  - Validates rental exists in database
  - Validates rental amount is positive

#### VerifyPaymentRequest
- `rentalId`: @NotNull (must be provided)
- `razorpayOrderId`: @NotBlank (non-empty string)
- `razorpayPaymentId`: @NotBlank (non-empty string)
- `razorpaySignature`: @NotBlank (non-empty string)

**Normalization**: All Razorpay identifiers are trimmed before processing to handle whitespace

### Signature Verification

**Process**:
1. Client receives payment confirmation from Razorpay with signature
2. Razorpay generates signature: HMAC-SHA256("orderid|paymentid", secret)
3. Client sends signature back to server
4. Server reconstructs payload: orderId + "|" + paymentId
5. Server generates expected signature using same secret
6. Compares signatures using constant-time algorithm

**Algorithm**: HMAC-SHA256
- **Key**: Razorpay key-secret (configured in application-dev.properties)
- **Data**: Concatenation of orderId and paymentId separated by "|"
- **Output**: Hexadecimal encoded 32-byte hash

**Comparison Security**:
- Uses `MessageDigest.isEqual()` for constant-time comparison
- Prevents timing-based attacks that could leak information
- Case-insensitive comparison (lowercase normalization)

### Credential Management

**Razorpay Credentials Stored In**:
- `src/main/resources/application-dev.properties`
- Configured via `RazorpayProperties` @ConfigurationProperties

**Validation**:
- Credentials checked for:
  - Non-null
  - Non-blank
  - Not placeholder values ("rzp_test_replace_me", "replace_me")
- Validation occurs before each API call

**Best Practices**:
- Use environment variables for production
- Never commit actual credentials to version control
- Rotate keys periodically
- Use test credentials in development (rzp_test_*)

---

## Configuration

### Razorpay Configuration

**File**: `src/main/resources/application-dev.properties`

```properties
# Razorpay API Credentials
razorpay.key-id=rzp_test_SVZgZiXtk4jDnZ
razorpay.key-secret=MSEzdHKzUVaOyS5HrbZzar2E
razorpay.currency=INR
```

**Property Details**:

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| razorpay.key-id | String | Yes | - | Razorpay account key ID (test or live) |
| razorpay.key-secret | String | Yes | - | Razorpay account key secret for signing |
| razorpay.currency | String | No | INR | Currency code for payments |

### Logging Configuration

**Logger**: `com.localproject.rental_local.service.PaymentService`
**Level**: INFO

**Logged Events**:
- Order creation request received
- Payment order created successfully
- Payment verification request received
- Signature verification success
- Signature verification failure

---

## Database Schema

### payments Table

```sql
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rental_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(10) NOT NULL,
    gateway_ref VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    gateway_signature VARCHAR(255),
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (rental_id) REFERENCES rentals(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    
    INDEX idx_rental_id (rental_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_gateway_ref (gateway_ref)
);
```

### Column Descriptions

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGINT | NO | Primary key, auto-increment |
| rental_id | BIGINT | NO | Foreign key to rentals table (unique) |
| user_id | BIGINT | NO | Foreign key to users table |
| amount | DECIMAL(10,2) | NO | Payment amount in rupees |
| status | VARCHAR(10) | NO | PENDING, SUCCESS, or FAILED |
| gateway_ref | VARCHAR(100) | YES | Razorpay order ID |
| gateway_payment_id | VARCHAR(100) | YES | Razorpay payment ID |
| gateway_signature | VARCHAR(255) | YES | HMAC signature from Razorpay |
| paid_at | TIMESTAMP | YES | Timestamp when payment succeeded |
| created_at | TIMESTAMP | NO | Record creation timestamp |
| updated_at | TIMESTAMP | NO | Record last update timestamp |

### Relationships

```
payments (Many) ←→ (One) rentals
  └─ One-to-One relationship
  └─ Unique constraint on rental_id
  └─ Cannot have multiple payments for same rental

payments (Many) ←→ (One) users
  └─ Many-to-One relationship
  └─ One user can have multiple payments
```

---

## Complete Request-Response Examples

### Example 1: Successful Payment Flow

**1. Create Order**

**Request**:
```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -d '{"rentalId": 1}'
```

**Response** (201):
```json
{
  "paymentId": 5,
  "rentalId": 1,
  "razorpayOrderId": "order_IluGWxBBWT7wjJ",
  "amount": 2500.00,
  "currency": "INR",
  "status": "PENDING"
}
```

**2. Verify Payment** (after user completes payment on Razorpay)

**Request**:
```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "rentalId": 1,
    "razorpayOrderId": "order_IluGWxBBWT7wjJ",
    "razorpayPaymentId": "pay_IluGWxBBWT7wjJ",
    "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
  }'
```

**Response** (200):
```json
{
  "paymentId": 5,
  "rentalId": 1,
  "razorpayOrderId": "order_IluGWxBBWT7wjJ",
  "razorpayPaymentId": "pay_IluGWxBBWT7wjJ",
  "status": "SUCCESS",
  "verified": true
}
```

**Database State After Verification**:
```
Payment Record:
- id: 5
- rental_id: 1
- user_id: <user_id>
- amount: 2500.00
- status: SUCCESS
- gateway_ref: order_IluGWxBBWT7wjJ
- gateway_payment_id: pay_IluGWxBBWT7wjJ
- gateway_signature: 9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d
- paid_at: 2026-03-26 15:30:45
- created_at: 2026-03-26 15:25:30
- updated_at: 2026-03-26 15:30:45
```

---

### Example 2: Signature Verification Failure

**Request**:
```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -d '{
    "rentalId": 1,
    "razorpayOrderId": "order_IluGWxBBWT7wjJ",
    "razorpayPaymentId": "pay_IluGWxBBWT7wjJ",
    "razorpaySignature": "invalid_signature_tampering_detected"
  }'
```

**Response** (401):
```json
{
  "timestamp": "2026-03-26T15:31:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid Razorpay signature",
  "path": "/api/payments/verify"
}
```

**Database State**:
```
Payment Record (Updated):
- status: FAILED
- paid_at: null
- gateway_payment_id: pay_IluGWxBBWT7wjJ
- gateway_signature: invalid_signature_tampering_detected
```

**Audit Log**:
```
[WARN] Payment signature verification failed: 
  paymentId=5, rentalId=1, 
  orderId=order_IluGWxBBWT7wjJ, 
  paymentGatewayId=pay_IluGWxBBWT7wjJ
```

---

## Summary

The Payment Module provides a secure, end-to-end payment processing system integrated with Razorpay. Key aspects:

1. **Two-Step Process**: Create order → Verify signature
2. **Security**: HMAC-SHA256 signature verification with constant-time comparison
3. **Status Tracking**: PENDING → SUCCESS or FAILED states
4. **Audit Trail**: Comprehensive logging of all operations
5. **Error Handling**: Specific exceptions for different failure scenarios
6. **Configuration**: Environment-based credential management
7. **Data Persistence**: Complete payment record storage with timestamps

The module follows Spring Boot best practices including:
- Separation of concerns (Controller → Service → Repository)
- DTOs for API contracts
- Exception handling with meaningful messages
- Comprehensive logging for debugging
- Input validation with annotations
- Integration with external payment gateway

---

**Document Created**: March 26, 2026
**Version**: 1.0
**Application**: Rental Local - Spring Boot Udemy Milestone Project

