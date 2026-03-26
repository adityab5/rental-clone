package com.localproject.rental_local.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentOrderRequest(
        @NotNull Long rentalId
) {
}

