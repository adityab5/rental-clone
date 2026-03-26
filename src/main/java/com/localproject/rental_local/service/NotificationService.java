package com.localproject.rental_local.service;

import com.localproject.rental_local.config.NotificationMailProperties;
import com.localproject.rental_local.dto.response.NotificationResponse;
import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PaymentRepository paymentRepository;
    private final JavaMailSender mailSender;
    private final NotificationMailProperties notificationMailProperties;

    public NotificationResponse sendPaymentSuccessNotification(Long rentalId) {
        Payment payment = paymentRepository.findByRentalId(rentalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found for rental id: " + rentalId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not successful for rental id: " + rentalId);
        }

        sendPaymentSuccessEmail(payment);

        return new NotificationResponse(
                payment.getRental().getId(),
                payment.getId(),
                payment.getUser().getEmail(),
                "SENT",
                "Payment success confirmation email sent"
        );
    }

    public void sendPaymentSuccessEmail(Payment payment) {
        if (!notificationMailProperties.isEnabled()) {
            log.info("Notification email disabled. Skipping payment success email for paymentId={}", payment.getId());
            return;
        }

        String recipientEmail = payment.getUser().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient email is missing for payment id: " + payment.getId());
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationMailProperties.getFrom());
        message.setTo(recipientEmail.trim());
        message.setSubject("Payment Confirmation - Rental #" + payment.getRental().getId());
        message.setText(buildPaymentSuccessBody(payment));

        try {
            mailSender.send(message);
            log.info("Payment success email sent: paymentId={}, rentalId={}, recipient={}",
                    payment.getId(), payment.getRental().getId(), recipientEmail);
        } catch (MailException exception) {
            log.error("Failed sending payment confirmation email for paymentId={}, rentalId={}",
                    payment.getId(), payment.getRental().getId(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to send payment confirmation email");
        }
    }

    private String buildPaymentSuccessBody(Payment payment) {
        return "Hi " + payment.getUser().getName() + ",\n\n"
                + "Your payment was successful.\n"
                + "Rental ID: " + payment.getRental().getId() + "\n"
                + "Payment ID: " + payment.getId() + "\n"
                + "Gateway Order ID: " + payment.getGatewayRef() + "\n"
                + "Amount: " + payment.getAmount() + "\n"
                + "Status: " + payment.getStatus() + "\n\n"
                + "Thank you for using Rental Local.";
    }
}
