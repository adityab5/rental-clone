package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localproject.rental_local.config.NotificationMailProperties;
import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.repository.PaymentRepository;
import com.localproject.rental_local.service.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationMailProperties notificationMailProperties;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(paymentRepository, mailSender, notificationMailProperties);
    }

    @Test
    void shouldSendPaymentSuccessNotification() {
        Payment payment = samplePayment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByRentalId(1L)).thenReturn(Optional.of(payment));
        when(notificationMailProperties.isEnabled()).thenReturn(true);
        when(notificationMailProperties.getFrom()).thenReturn("noreply@test.com");

        var response = notificationService.sendPaymentSuccessNotification(1L);

        assertEquals("SENT", response.status());
        assertEquals("buyer@test.com", response.recipientEmail());
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void shouldRejectNotificationIfPaymentNotSuccessful() {
        Payment payment = samplePayment(PaymentStatus.PENDING);
        when(paymentRepository.findByRentalId(1L)).thenReturn(Optional.of(payment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> notificationService.sendPaymentSuccessNotification(1L)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    private Payment samplePayment(PaymentStatus status) {
        User user = new User();
        user.setId(10L);
        user.setName("Buyer User");
        user.setEmail("buyer@test.com");

        Rental rental = new Rental();
        rental.setId(1L);
        rental.setUser(user);
        rental.setStartDate(LocalDate.now());
        rental.setEndDate(LocalDate.now().plusDays(1));

        return Payment.builder()
                .id(11L)
                .rental(rental)
                .user(user)
                .amount(new BigDecimal("500.00"))
                .status(status)
                .gatewayRef("order_test_1")
                .build();
    }
}

