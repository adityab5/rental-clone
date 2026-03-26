package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.request.CreatePaymentOrderRequest;
import com.localproject.rental_local.dto.request.VerifyPaymentRequest;
import com.localproject.rental_local.dto.response.CreatePaymentOrderResponse;
import com.localproject.rental_local.dto.response.VerifyPaymentResponse;
import com.localproject.rental_local.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentOrderResponse createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        log.info("Received create-order request for rentalId={}", request.rentalId());
        return paymentService.createOrder(request);
    }

    @PostMapping("/verify")
    public VerifyPaymentResponse verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        log.info("Received verify request for rentalId={}, orderId={}", request.rentalId(), request.razorpayOrderId());
        return paymentService.verifyPayment(request);
    }
}
