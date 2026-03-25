package com.localproject.rental_local.dto;

import java.math.BigDecimal;

public record AdminDashboardDto(
        long totalUsers,
        long activeUsers,
        long deletedUsers,
        long totalRentals,
        long activeRentals,
        long overdueRentals,
        long totalPayments,
        long successPayments,
        long pendingPayments,
        BigDecimal totalRevenue,
        BigDecimal pendingRevenue
) {
}

