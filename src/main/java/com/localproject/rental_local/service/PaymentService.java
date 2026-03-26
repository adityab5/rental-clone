package com.localproject.rental_local.service;

import com.localproject.rental_local.config.RazorpayProperties;
import com.localproject.rental_local.dto.CreatePaymentOrderRequest;
import com.localproject.rental_local.dto.CreatePaymentOrderResponse;
import com.localproject.rental_local.dto.VerifyPaymentRequest;
import com.localproject.rental_local.dto.VerifyPaymentResponse;
import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.exception.InvalidPaymentSignatureException;
import com.localproject.rental_local.exception.PaymentNotFoundException;
import com.localproject.rental_local.exception.PaymentOperationException;
import com.localproject.rental_local.repository.PaymentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.service.gateway.RazorpayGatewayClient;
import com.localproject.rental_local.service.gateway.RazorpayOrderResult;
import com.localproject.rental_local.util.HmacSHA256Util;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayGatewayClient razorpayGatewayClient;
    private final RazorpayProperties razorpayProperties;
    private final HmacSHA256Util hmacSHA256Util;

    public CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new PaymentNotFoundException("Rental not found for id: " + request.rentalId()));

        BigDecimal amount = rental.getTotalCost();
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentOperationException("Rental amount is invalid for payment");
        }

        Payment payment = paymentRepository.findByRentalId(rental.getId())
                .orElseGet(() -> Payment.builder()
                        .rental(rental)
                        .user(rental.getUser())
                        .build());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentOperationException("Payment already marked successful for rental id: " + rental.getId());
        }

        RazorpayOrderResult orderResult = razorpayGatewayClient.createOrder(rental.getId(), amount, razorpayProperties.getCurrency());

        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayRef(orderResult.orderId());
        payment.setGatewayPaymentId(null);
        payment.setGatewaySignature(null);
        payment.setPaidAt(null);

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment order created and persisted: paymentId={}, rentalId={}, orderId={}",
                savedPayment.getId(), rental.getId(), orderResult.orderId());

        return new CreatePaymentOrderResponse(
                savedPayment.getId(),
                rental.getId(),
                orderResult.orderId(),
                amount,
                orderResult.currency(),
                savedPayment.getStatus().name()
        );
    }

    public VerifyPaymentResponse verifyPayment(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRentalId(request.rentalId())
                .orElseThrow(() -> new PaymentNotFoundException("No payment initiated for rental id: " + request.rentalId()));

        String requestOrderId = request.razorpayOrderId() == null ? null : request.razorpayOrderId().trim();
        String requestPaymentId = request.razorpayPaymentId() == null ? null : request.razorpayPaymentId().trim();
        String requestSignature = request.razorpaySignature() == null ? null : request.razorpaySignature().trim();

        String persistedOrderId = payment.getGatewayRef();
        if (persistedOrderId == null || persistedOrderId.isBlank()) {
            throw new PaymentOperationException("Persisted order id is missing for rental id: " + request.rentalId());
        }
        persistedOrderId = persistedOrderId.trim();

        if (!persistedOrderId.equals(requestOrderId)) {
            throw new PaymentOperationException("Order id mismatch for rental id: " + request.rentalId());
        }

        // Razorpay signs "order_id|payment_id" using key_secret.
        String payload = requestOrderId + "|" + requestPaymentId;
        boolean valid = hmacSHA256Util.isValidSignature(payload, requestSignature, razorpayProperties.getKeySecret());

        payment.setGatewayPaymentId(requestPaymentId);
        payment.setGatewaySignature(requestSignature);

        if (!valid) {
            // Persist failed attempt for audit/debugging before returning validation error.
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaidAt(null);
            paymentRepository.save(payment);
            log.warn("Payment signature verification failed: paymentId={}, rentalId={}, orderId={}, paymentGatewayId={}",
                    payment.getId(), payment.getRental().getId(), requestOrderId, requestPaymentId);
            throw new InvalidPaymentSignatureException("Invalid Razorpay signature");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment signature verification success: paymentId={}, rentalId={}, orderId={}, paymentGatewayId={}",
                savedPayment.getId(), savedPayment.getRental().getId(), requestOrderId, requestPaymentId);

        return new VerifyPaymentResponse(
                savedPayment.getId(),
                savedPayment.getRental().getId(),
                requestOrderId,
                requestPaymentId,
                savedPayment.getStatus(),
                true
        );
    }
}



