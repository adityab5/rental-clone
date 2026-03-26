package com.localproject.rental_local.dto.response;

import com.localproject.rental_local.enums.EquipmentCategory;
import java.math.BigDecimal;

public record EquipmentDto(
        Long id,
        String name,
        EquipmentCategory category,
        BigDecimal dailyRate,
        Boolean isAvailable
) {
}


