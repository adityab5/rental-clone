package com.localproject.rental_local.service.gateway;

public record RazorpayOrderResult(
        String orderId,
        String currency,
        long amountInPaise
) {
}

