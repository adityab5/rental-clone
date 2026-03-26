package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.request.SendPaymentSuccessNotificationRequest;
import com.localproject.rental_local.dto.response.NotificationResponse;
import com.localproject.rental_local.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/")
    public NotificationResponse sendPaymentSuccessNotification(@Valid @RequestBody SendPaymentSuccessNotificationRequest request) {
        return notificationService.sendPaymentSuccessNotification(request.rentalId());
    }
}

