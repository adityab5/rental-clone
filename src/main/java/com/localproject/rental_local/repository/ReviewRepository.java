package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByEquipmentIdOrderByCreatedAtDesc(Long equipmentId);

    Optional<Review> findByUserIdAndEquipmentId(Long userId, Long equipmentId);

    long countByEquipmentId(Long equipmentId);
}

