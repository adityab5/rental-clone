package com.localproject.rental_local.dto.request;

import jakarta.validation.constraints.NotNull;

public record SendPaymentSuccessNotificationRequest(
        @NotNull(message = "rentalId is required")
        Long rentalId
) {
}

