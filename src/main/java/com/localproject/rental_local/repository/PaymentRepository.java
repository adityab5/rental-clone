package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRentalId(Long rentalId);

    List<Payment> findAllByUserIdAndStatus(Long userId, PaymentStatus status);

    Optional<Payment> findByGatewayRef(String gatewayRef);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(PaymentStatus status);
}

