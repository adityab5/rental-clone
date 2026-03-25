package com.localproject.rental_local.service;

import com.localproject.rental_local.dto.CreateRentalRequest;
import com.localproject.rental_local.dto.RentalDto;
import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.RentalStatus;
import com.localproject.rental_local.repository.EquipmentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;

    public RentalDto createRental(CreateRentalRequest request) {
        validateDateRange(request.startDate(), request.endDate());

        User user = userRepository.findByIdAndIsDeletedFalse(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(request.equipmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        if (!Boolean.TRUE.equals(equipment.getIsAvailable())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment is not available");
        }

        boolean overlapsActiveRental = rentalRepository.existsByEquipmentIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                equipment.getId(),
                RentalStatus.ACTIVE,
                request.startDate(),
                request.endDate()
        );

        if (overlapsActiveRental) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment is already booked in this date range");
        }

        Rental rental = Rental.builder()
                .user(user)
                .equipment(equipment)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(RentalStatus.ACTIVE)
                .totalCost(calculateTotalCost(equipment.getDailyRate(), request.startDate(), request.endDate()))
                .build();

        equipment.setIsAvailable(false);
        equipmentRepository.save(equipment);

        return toDto(rentalRepository.save(rental));
    }

    public RentalDto getRentalById(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found"));
        return toDto(rental);
    }

    public RentalDto markRentalAsReturned(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental not found"));

        if (rental.getStatus() == RentalStatus.RETURNED) {
            return toDto(rental);
        }

        rental.setStatus(RentalStatus.RETURNED);
        rental.setReturnDate(LocalDate.now());

        Equipment equipment = rental.getEquipment();
        equipment.setIsAvailable(true);
        equipmentRepository.save(equipment);

        return toDto(rentalRepository.save(rental));
    }

    public List<RentalDto> listAllRentals() {
        return rentalRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toDto)
                .toList();
    }

    public List<RentalDto> listOverdueRentals() {
        return rentalRepository.findAllByStatusAndEndDateBefore(RentalStatus.ACTIVE, LocalDate.now()).stream()
                .map(this::toDto)
                .toList();
    }

    private BigDecimal calculateTotalCost(BigDecimal dailyRate, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return dailyRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }
    }

    private RentalDto toDto(Rental rental) {
        return new RentalDto(
                rental.getId(),
                rental.getUser().getId(),
                rental.getEquipment().getId(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getReturnDate(),
                rental.getStatus(),
                rental.getTotalCost(),
                rental.getCreatedAt()
        );
    }
}

