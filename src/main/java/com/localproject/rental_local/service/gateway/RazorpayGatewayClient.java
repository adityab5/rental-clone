package com.localproject.rental_local.service.gateway;

import java.math.BigDecimal;

public interface RazorpayGatewayClient {

    RazorpayOrderResult createOrder(Long rentalId, BigDecimal amount, String currency);
}

