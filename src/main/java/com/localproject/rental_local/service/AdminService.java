package com.localproject.rental_local.service;

import com.localproject.rental_local.dto.response.AdminDashboardDto;
import com.localproject.rental_local.dto.response.AdminUserDto;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.enums.RentalStatus;
import com.localproject.rental_local.repository.PaymentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAdminUserDto)
                .toList();
    }

    public AdminUserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toAdminUserDto(user);
    }

    public AdminUserDto deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            return toAdminUserDto(user);
        }

        user.setIsDeleted(true);
        return toAdminUserDto(userRepository.save(user));
    }

    public AdminDashboardDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsDeletedFalse();
        long deletedUsers = userRepository.countByIsDeletedTrue();

        long totalRentals = rentalRepository.count();
        long activeRentals = rentalRepository.countByStatus(RentalStatus.ACTIVE);
        long overdueRentals = rentalRepository.countByStatusAndEndDateBefore(RentalStatus.ACTIVE, LocalDate.now());

        long totalPayments = paymentRepository.count();
        long successPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);

        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        BigDecimal pendingRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.PENDING);

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        if (pendingRevenue == null) {
            pendingRevenue = BigDecimal.ZERO;
        }

        return new AdminDashboardDto(
                totalUsers,
                activeUsers,
                deletedUsers,
                totalRentals,
                activeRentals,
                overdueRentals,
                totalPayments,
                successPayments,
                pendingPayments,
                totalRevenue,
                pendingRevenue
        );
    }

    private AdminUserDto toAdminUserDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsDeleted(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
