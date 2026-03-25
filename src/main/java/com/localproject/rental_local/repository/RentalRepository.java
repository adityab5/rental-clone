package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.Rental;
import com.localproject.rental_local.enums.RentalStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Rental> findAllByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);

    List<Rental> findAllByStatusAndEndDateBefore(RentalStatus status, LocalDate date);

    boolean existsByEquipmentIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            Long equipmentId,
            RentalStatus status,
            LocalDate startDate,
            LocalDate endDate
    );
}

