package com.localproject.rental_local.dto;

import com.localproject.rental_local.enums.RentalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalDto(
        Long id,
        Long userId,
        Long equipmentId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate returnDate,
        RentalStatus status,
        BigDecimal totalCost,
        LocalDateTime createdAt
) {
}

