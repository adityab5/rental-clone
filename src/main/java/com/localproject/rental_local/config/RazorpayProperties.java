package com.localproject.rental_local.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    @NotBlank
    private String keyId;

    @NotBlank
    private String keySecret;

    @NotBlank
    private String currency = "INR";
}

