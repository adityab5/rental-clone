package com.localproject.rental_local.dto;

import java.math.BigDecimal;

public record CreatePaymentOrderResponse(
        Long paymentId,
        Long rentalId,
        String razorpayOrderId,
        BigDecimal amount,
        String currency,
        String status
) {
}

