package com.localproject.rental_local.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequest(
        @NotNull Long userId,
        @NotNull Long equipmentId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}


