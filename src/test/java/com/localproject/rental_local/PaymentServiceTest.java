package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.localproject.rental_local.config.RazorpayProperties;
import com.localproject.rental_local.dto.request.CreatePaymentOrderRequest;
import com.localproject.rental_local.dto.request.VerifyPaymentRequest;
import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.exception.InvalidPaymentSignatureException;
import com.localproject.rental_local.repository.PaymentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.service.NotificationService;
import com.localproject.rental_local.service.PaymentService;
import com.localproject.rental_local.service.gateway.RazorpayGatewayClient;
import com.localproject.rental_local.service.gateway.RazorpayOrderResult;
import com.localproject.rental_local.util.HmacSHA256Util;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayGatewayClient razorpayGatewayClient;

    @Mock
    private RazorpayProperties razorpayProperties;

    @Mock
    private HmacSHA256Util hmacSHA256Util;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreateOrderAndPersistPendingPayment() {
        Rental rental = sampleRental(1L, new BigDecimal("500.00"));
        when(razorpayProperties.getCurrency()).thenReturn("INR");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalId(1L)).thenReturn(Optional.empty());
        when(razorpayGatewayClient.createOrder(1L, new BigDecimal("500.00"), "INR"))
                .thenReturn(new RazorpayOrderResult("order_test_1", "INR", 50000L));
        when(paymentRepository.save(org.mockito.ArgumentMatchers.any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(11L);
            return payment;
        });

        var response = paymentService.createOrder(new CreatePaymentOrderRequest(1L));

        assertEquals(11L, response.paymentId());
        assertEquals("order_test_1", response.razorpayOrderId());
        assertEquals("PENDING", response.status());
    }

    @Test
    void shouldVerifyPaymentAndMarkSuccess() {
        Rental rental = sampleRental(1L, new BigDecimal("500.00"));
        when(razorpayProperties.getKeySecret()).thenReturn("secret");
        Payment payment = Payment.builder()
                .id(11L)
                .rental(rental)
                .user(rental.getUser())
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PENDING)
                .gatewayRef("order_test_1")
                .build();

        when(paymentRepository.findByRentalId(1L)).thenReturn(Optional.of(payment));
        when(hmacSHA256Util.isValidSignature("order_test_1|pay_test_1", "sig_test_1", "secret")).thenReturn(true);
        when(paymentRepository.save(payment)).thenReturn(payment);

        var response = paymentService.verifyPayment(new VerifyPaymentRequest(1L, "order_test_1", "pay_test_1", "sig_test_1"));

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals(true, response.verified());
        verify(notificationService).sendPaymentSuccessEmail(payment);
    }

    @Test
    void shouldRejectInvalidSignature() {
        Rental rental = sampleRental(1L, new BigDecimal("500.00"));
        when(razorpayProperties.getKeySecret()).thenReturn("secret");
        Payment payment = Payment.builder()
                .id(11L)
                .rental(rental)
                .user(rental.getUser())
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.PENDING)
                .gatewayRef("order_test_1")
                .build();

        when(paymentRepository.findByRentalId(1L)).thenReturn(Optional.of(payment));
        when(hmacSHA256Util.isValidSignature("order_test_1|pay_test_1", "sig_test_1", "secret")).thenReturn(false);

        assertThrows(
                InvalidPaymentSignatureException.class,
                () -> paymentService.verifyPayment(new VerifyPaymentRequest(1L, "order_test_1", "pay_test_1", "sig_test_1"))
        );
        verify(notificationService, never()).sendPaymentSuccessEmail(payment);
    }

    private Rental sampleRental(Long rentalId, BigDecimal totalCost) {
        User user = new User();
        user.setId(100L);

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(user);
        rental.setStartDate(LocalDate.now());
        rental.setEndDate(LocalDate.now().plusDays(1));
        rental.setTotalCost(totalCost);
        return rental;
    }
}
