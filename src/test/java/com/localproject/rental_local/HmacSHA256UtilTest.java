package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.localproject.rental_local.util.HmacSHA256Util;
import org.junit.jupiter.api.Test;

class HmacSHA256UtilTest {

    private final HmacSHA256Util hmacSHA256Util = new HmacSHA256Util();

    @Test
    void shouldGenerateStableSignature() {
        String payload = "order_test|payment_test";
        String secret = "test_secret";

        String signature1 = hmacSHA256Util.generateSignature(payload, secret);
        String signature2 = hmacSHA256Util.generateSignature(payload, secret);

        assertNotNull(signature1);
        assertFalse(signature1.isEmpty());
        assertEquals(signature1, signature2);
    }

    @Test
    void shouldValidateSignatureCorrectly() {
        String payload = "order_123|pay_123";
        String secret = "secret_123";
        String validSignature = hmacSHA256Util.generateSignature(payload, secret);

        assertTrue(hmacSHA256Util.isValidSignature(payload, validSignature, secret));
        assertFalse(hmacSHA256Util.isValidSignature(payload, "invalid_signature", secret));
    }
}


