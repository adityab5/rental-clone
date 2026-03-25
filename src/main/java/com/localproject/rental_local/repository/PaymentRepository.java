package com.localproject.rental_local.repository;

import com.localproject.rental_local.entity.Payment;
import com.localproject.rental_local.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRentalId(Long rentalId);

    List<Payment> findAllByUserIdAndStatus(Long userId, PaymentStatus status);

    Optional<Payment> findByGatewayRef(String gatewayRef);
}

