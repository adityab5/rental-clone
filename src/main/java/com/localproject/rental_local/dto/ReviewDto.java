package com.localproject.rental_local.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        Long userId,
        Long equipmentId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}

