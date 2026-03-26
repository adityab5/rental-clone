# Rental Local

Spring Boot rental management API.

## Available Equipment API

- `GET /api/equipment/available`
- Optional query param: `category` (`VEHICLE`, `ELECTRONIC`, `FURNITURE`)

### Example

- `GET /api/equipment/available`
- `GET /api/equipment/available?category=VEHICLE`

## Rental API

- `POST /api/rentals/` - Create a new rental (availability check + total cost calculation)
- `GET /api/rentals/{id}` - Get rental detail
- `POST /api/rentals/{id}/return` - Mark rental as returned
- `GET /api/rentals/` - List all rentals
- `GET /api/rentals/overdue` - List overdue rentals (active rentals with `end_date < today`)

### Rental Create Example

```json
{
  "userId": 1,
  "equipmentId": 10,
  "startDate": "2026-03-25",
  "endDate": "2026-03-27"
}
```

## Payment API (Razorpay Backend Only)

- `POST /api/payments/create-order` - Create Razorpay order and persist pending payment
- `POST /api/payments/verify` - Verify signature (`order_id|payment_id`) and update status

### Create Order Example

```json
{
  "rentalId": 1
}
```

### Verify Payment Example (Postman Simulation)

```json
{
  "rentalId": 1,
  "razorpayOrderId": "order_abc123",
  "razorpayPaymentId": "pay_xyz789",
  "razorpaySignature": "<hmac_sha256_signature>"
}
```

### Razorpay Config

Set credentials via environment variables or `application-dev.properties`:

- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_CURRENCY` (default: `INR`)

## What Was Added

- Entity repositories under `src/main/java/com/localproject/rental_local/repository`
- Available equipment endpoint:
  - `src/main/java/com/localproject/rental_local/controller/EquipmentController.java`
  - `src/main/java/com/localproject/rental_local/service/EquipmentService.java`
  - `src/main/java/com/localproject/rental_local/dto/EquipmentDto.java`
- Rental module:
  - `src/main/java/com/localproject/rental_local/controller/RentalController.java`
  - `src/main/java/com/localproject/rental_local/service/RentalService.java`
  - `src/main/java/com/localproject/rental_local/dto/CreateRentalRequest.java`
  - `src/main/java/com/localproject/rental_local/dto/RentalDto.java`
- Controller test:
  - `src/test/java/com/localproject/rental_local/EquipmentControllerTest.java`
  - `src/test/java/com/localproject/rental_local/RentalServiceTest.java`

## Run Tests

```powershell
.\gradlew.bat test --no-daemon
```

