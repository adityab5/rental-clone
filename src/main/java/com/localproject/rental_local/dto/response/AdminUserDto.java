package com.localproject.rental_local.dto.response;

import com.localproject.rental_local.enums.Role;
import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


