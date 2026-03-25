package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.localproject.rental_local.dto.AdminDashboardDto;
import com.localproject.rental_local.dto.AdminUserDto;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.PaymentStatus;
import com.localproject.rental_local.enums.Role;
import com.localproject.rental_local.enums.RentalStatus;
import com.localproject.rental_local.repository.PaymentRepository;
import com.localproject.rental_local.repository.RentalRepository;
import com.localproject.rental_local.repository.UserRepository;
import com.localproject.rental_local.service.AdminService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private AdminService adminService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        adminService = new AdminService(userRepository, rentalRepository, paymentRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldGetAllUsers() {
        User user1 = sampleUser(1L, "user1@test.com", false);
        User user2 = sampleUser(2L, "user2@test.com", true);

        when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user1, user2));

        var result = adminService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertFalse(result.get(0).isDeleted());
    }

    @Test
    void shouldGetUserById() {
        User user = sampleUser(1L, "user1@test.com", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = adminService.getUserById(1L);

        assertEquals(1L, result.id());
        assertEquals("user1@test.com", result.email());
        assertFalse(result.isDeleted());
    }

    @Test
    void shouldThrow404WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.getUserById(999L)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void shouldDeactivateUser() {
        User user = sampleUser(1L, "user1@test.com", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));

        var result = adminService.deactivateUser(1L);

        assertEquals(1L, result.id());
    }

    @Test
    void shouldProvideDashboardStats() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsDeletedFalse()).thenReturn(8L);
        when(userRepository.countByIsDeletedTrue()).thenReturn(2L);

        when(rentalRepository.count()).thenReturn(25L);
        when(rentalRepository.countByStatus(RentalStatus.ACTIVE)).thenReturn(5L);
        when(rentalRepository.countByStatusAndEndDateBefore(RentalStatus.ACTIVE, LocalDate.now())).thenReturn(2L);

        when(paymentRepository.count()).thenReturn(20L);
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(15L);
        when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(5L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("15000.00"));
        when(paymentRepository.sumAmountByStatus(PaymentStatus.PENDING)).thenReturn(new BigDecimal("2500.00"));

        AdminDashboardDto result = adminService.getDashboardStats();

        assertEquals(10L, result.totalUsers());
        assertEquals(8L, result.activeUsers());
        assertEquals(2L, result.deletedUsers());
        assertEquals(25L, result.totalRentals());
        assertEquals(5L, result.activeRentals());
        assertEquals(2L, result.overdueRentals());
        assertEquals(20L, result.totalPayments());
        assertEquals(15L, result.successPayments());
        assertEquals(5L, result.pendingPayments());
        assertEquals(new BigDecimal("15000.00"), result.totalRevenue());
        assertEquals(new BigDecimal("2500.00"), result.pendingRevenue());
    }

    private User sampleUser(Long userId, String email, Boolean isDeleted) {
        User user = new User();
        user.setId(userId);
        user.setName("User " + userId);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(Role.USER);
        user.setIsDeleted(isDeleted);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}

