package com.localproject.rental_local.util;

import com.localproject.rental_local.exception.PaymentOperationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSHA256Util {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    public String generateSignature(String data, String secret) {
        if (data == null || data.isBlank()) {
            throw new PaymentOperationException("Signature payload cannot be empty");
        }
        if (secret == null || secret.isBlank()) {
            throw new PaymentOperationException("Payment secret is missing");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception exception) {
            throw new PaymentOperationException("Unable to generate HMAC SHA256 signature");
        }
    }

    public boolean isValidSignature(String payload, String providedSignature, String secret) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }

        String expectedSignature = generateSignature(payload, secret == null ? null : secret.trim());
        String normalizedProvided = providedSignature.trim().toLowerCase(Locale.ROOT);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                normalizedProvided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}


