package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.enums.EquipmentCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllBySellerIdAndIsDeletedFalse(Long sellerId);

    List<Equipment> findAllByCategoryAndIsAvailableTrueAndIsDeletedFalse(EquipmentCategory category);

    Optional<Equipment> findByIdAndIsDeletedFalse(Long id);

    List<Equipment> findAllByIsAvailableTrueAndIsDeletedFalse();

    List<Equipment> findAllByDailyRateBetweenAndIsDeletedFalse(BigDecimal minDailyRate, BigDecimal maxDailyRate);
}
