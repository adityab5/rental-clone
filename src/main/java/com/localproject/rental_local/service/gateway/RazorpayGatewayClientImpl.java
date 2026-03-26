package com.localproject.rental_local.service.gateway;

import com.localproject.rental_local.config.RazorpayProperties;
import com.localproject.rental_local.exception.PaymentGatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayGatewayClientImpl implements RazorpayGatewayClient {

    private final RazorpayProperties razorpayProperties;

    @Override
    public RazorpayOrderResult createOrder(Long rentalId, BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentGatewayException("Amount must be greater than zero");
        }

        String keyId = razorpayProperties.getKeyId();
        String keySecret = razorpayProperties.getKeySecret();
        if (isPlaceholder(keyId) || isPlaceholder(keySecret)) {
            throw new PaymentGatewayException("Razorpay credentials are not configured. Set razorpay.key-id and razorpay.key-secret");
        }

        try {
            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

            long amountInPaise = amount
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            String receipt = "rental_" + rentalId + "_" + System.currentTimeMillis();
            JSONObject request = new JSONObject();
            request.put("amount", amountInPaise);
            request.put("currency", currency);
            request.put("receipt", receipt);

            Order order = razorpayClient.orders.create(request);
            String orderId = order.get("id").toString();
            String orderCurrency = order.get("currency").toString();
            long orderAmountInPaise = Long.parseLong(order.get("amount").toString());

            log.info("Razorpay order created: rentalId={}, orderId={}, amountPaise={}", rentalId, orderId, orderAmountInPaise);
            return new RazorpayOrderResult(orderId, orderCurrency, orderAmountInPaise);
        } catch (RazorpayException exception) {
            log.error("Failed to create Razorpay order for rentalId={}", rentalId, exception);
            throw new PaymentGatewayException("Failed to create Razorpay order", exception);
        }
    }

    private boolean isPlaceholder(String value) {
        return value == null
                || value.isBlank()
                || "rzp_test_replace_me".equals(value)
                || "replace_me".equals(value);
    }
}


