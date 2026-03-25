package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.EquipmentCategory;
import com.localproject.rental_local.repository.EquipmentRepository;
import com.localproject.rental_local.service.EquipmentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        equipmentService = new EquipmentService(equipmentRepository);
    }

    @Test
    void shouldReturnAvailableEquipmentWithoutCategoryFilter() {
        Equipment equipment = sampleEquipment(1L, "Projector", EquipmentCategory.ELECTRONIC, new BigDecimal("250.00"));
        when(equipmentRepository.findAllByIsAvailableTrueAndIsDeletedFalse()).thenReturn(List.of(equipment));

        var result = equipmentService.listAvailableEquipment(null);

        assertEquals(1, result.size());
        assertEquals("Projector", result.getFirst().name());
        assertEquals(EquipmentCategory.ELECTRONIC, result.getFirst().category());
        assertTrue(result.getFirst().isAvailable());
        verify(equipmentRepository).findAllByIsAvailableTrueAndIsDeletedFalse();
    }

    @Test
    void shouldReturnAvailableEquipmentWithCategoryFilter() {
        Equipment equipment = sampleEquipment(2L, "Scooter", EquipmentCategory.VEHICLE, new BigDecimal("500.00"));
        when(equipmentRepository.findAllByCategoryAndIsAvailableTrueAndIsDeletedFalse(eq(EquipmentCategory.VEHICLE)))
                .thenReturn(List.of(equipment));

        var result = equipmentService.listAvailableEquipment(EquipmentCategory.VEHICLE);

        assertEquals(1, result.size());
        assertEquals("Scooter", result.getFirst().name());
        assertEquals(EquipmentCategory.VEHICLE, result.getFirst().category());
        verify(equipmentRepository).findAllByCategoryAndIsAvailableTrueAndIsDeletedFalse(EquipmentCategory.VEHICLE);
    }

    private Equipment sampleEquipment(Long id, String name, EquipmentCategory category, BigDecimal dailyRate) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName(name);
        equipment.setCategory(category);
        equipment.setDailyRate(dailyRate);
        equipment.setIsAvailable(true);
        equipment.setIsDeleted(false);
        equipment.setSeller(new User());
        return equipment;
    }
}
