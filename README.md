# Rental Local

Spring Boot rental management API.

## Available Equipment API

- `GET /api/equipment/available`
- Optional query param: `category` (`VEHICLE`, `ELECTRONIC`, `FURNITURE`)

### Example

- `GET /api/equipment/available`
- `GET /api/equipment/available?category=VEHICLE`

## What Was Added

- Entity repositories under `src/main/java/com/localproject/rental_local/repository`
- Available equipment endpoint:
  - `src/main/java/com/localproject/rental_local/controller/EquipmentController.java`
  - `src/main/java/com/localproject/rental_local/service/EquipmentService.java`
  - `src/main/java/com/localproject/rental_local/dto/EquipmentDto.java`
- Controller test:
  - `src/test/java/com/localproject/rental_local/EquipmentControllerTest.java`

## Run Tests

```powershell
.\gradlew.bat test --no-daemon
```

