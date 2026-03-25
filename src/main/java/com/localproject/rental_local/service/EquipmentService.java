package com.localproject.rental_local.service;

import com.localproject.rental_local.dto.EquipmentDto;
import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.enums.EquipmentCategory;
import com.localproject.rental_local.repository.EquipmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public List<EquipmentDto> listAvailableEquipment(EquipmentCategory category) {
        List<Equipment> equipmentList = category == null
                ? equipmentRepository.findAllByIsAvailableTrueAndIsDeletedFalse()
                : equipmentRepository.findAllByCategoryAndIsAvailableTrueAndIsDeletedFalse(category);

        return equipmentList.stream()
                .map(this::toDto)
                .toList();
    }

    private EquipmentDto toDto(Equipment equipment) {
        return new EquipmentDto(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getDailyRate(),
                equipment.getIsAvailable()
        );
    }
}

