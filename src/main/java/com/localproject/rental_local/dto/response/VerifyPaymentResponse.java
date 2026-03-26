package com.localproject.rental_local.dto.response;

import com.localproject.rental_local.enums.PaymentStatus;

public record VerifyPaymentResponse(
        Long paymentId,
        Long rentalId,
        String razorpayOrderId,
        String razorpayPaymentId,
        PaymentStatus status,
        boolean verified
) {
}


