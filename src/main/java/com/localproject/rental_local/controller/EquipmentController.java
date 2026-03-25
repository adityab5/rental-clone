package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.EquipmentDto;
import com.localproject.rental_local.enums.EquipmentCategory;
import com.localproject.rental_local.service.EquipmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/")
    public List<EquipmentDto> getAvailableEquipment(
            @RequestParam(required = false) EquipmentCategory category
    ) {
        return equipmentService.listAvailableEquipment(category);
    }
}

