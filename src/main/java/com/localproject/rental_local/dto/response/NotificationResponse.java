package com.localproject.rental_local.dto.response;

public record NotificationResponse(
        Long rentalId,
        Long paymentId,
        String recipientEmail,
        String status,
        String message
) {
}

