package com.localproject.rental_local.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentOrderRequest(
        @NotNull Long rentalId
) {
}


