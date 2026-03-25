package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localproject.rental_local.dto.CreateRentalRequest;
import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.EquipmentCategory;
import com.localproject.rental_local.enums.RentalStatus;
import com.localproject.rental_local.enums.Role;
import com.localproject.rental_local.repository.EquipmentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.repository.UserRepository;
import com.localproject.rental_local.service.RentalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rentalService = new RentalService(rentalRepository, userRepository, equipmentRepository);
    }

    @Test
    void shouldCreateRentalWhenEquipmentIsAvailable() {
        CreateRentalRequest request = new CreateRentalRequest(
                11L,
                21L,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27)
        );

        User user = sampleUser(11L);
        Equipment equipment = sampleEquipment(21L, new BigDecimal("100.00"), true);

        when(userRepository.findByIdAndIsDeletedFalse(11L)).thenReturn(Optional.of(user));
        when(equipmentRepository.findByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(equipment));
        when(rentalRepository.existsByEquipmentIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                eq(21L), eq(RentalStatus.ACTIVE), eq(request.startDate()), eq(request.endDate())
        )).thenReturn(false);
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
            Rental saved = invocation.getArgument(0);
            saved.setId(101L);
            saved.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 0));
            return saved;
        });

        var result = rentalService.createRental(request);

        assertEquals(101L, result.id());
        assertEquals(new BigDecimal("300.00"), result.totalCost());
        assertEquals(RentalStatus.ACTIVE, result.status());
        verify(equipmentRepository).save(equipment);
        assertFalse(equipment.getIsAvailable());
    }

    @Test
    void shouldRejectCreateRentalWhenEquipmentAlreadyBooked() {
        CreateRentalRequest request = new CreateRentalRequest(
                11L,
                21L,
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 27)
        );

        when(userRepository.findByIdAndIsDeletedFalse(11L)).thenReturn(Optional.of(sampleUser(11L)));
        when(equipmentRepository.findByIdAndIsDeletedFalse(21L)).thenReturn(Optional.of(sampleEquipment(21L, new BigDecimal("100.00"), true)));
        when(rentalRepository.existsByEquipmentIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                eq(21L), eq(RentalStatus.ACTIVE), eq(request.startDate()), eq(request.endDate())
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> rentalService.createRental(request));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void shouldMarkRentalAsReturned() {
        Equipment equipment = sampleEquipment(21L, new BigDecimal("100.00"), false);
        Rental rental = Rental.builder()
                .id(301L)
                .user(sampleUser(11L))
                .equipment(equipment)
                .startDate(LocalDate.of(2026, 3, 20))
                .endDate(LocalDate.of(2026, 3, 24))
                .status(RentalStatus.ACTIVE)
                .totalCost(new BigDecimal("500.00"))
                .build();

        when(rentalRepository.findById(301L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = rentalService.markRentalAsReturned(301L);

        assertEquals(RentalStatus.RETURNED, result.status());
        assertNotNull(result.returnDate());
        verify(equipmentRepository).save(equipment);
        assertEquals(true, equipment.getIsAvailable());
    }

    private User sampleUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Demo User");
        user.setEmail("demo@example.com");
        user.setPassword("hashed");
        user.setRole(Role.USER);
        user.setIsDeleted(false);
        return user;
    }

    private Equipment sampleEquipment(Long id, BigDecimal dailyRate, boolean isAvailable) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setName("Camera");
        equipment.setCategory(EquipmentCategory.ELECTRONIC);
        equipment.setDailyRate(dailyRate);
        equipment.setIsAvailable(isAvailable);
        equipment.setIsDeleted(false);
        equipment.setSeller(sampleUser(99L));
        return equipment;
    }
}

