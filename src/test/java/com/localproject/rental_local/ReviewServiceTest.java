package com.localproject.rental_local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localproject.rental_local.dto.CreateReviewRequest;
import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.entity.Review;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.enums.EquipmentCategory;
import com.localproject.rental_local.enums.Role;
import com.localproject.rental_local.repository.EquipmentRepository;
import com.localproject.rental_local.repository.ReviewRepository;
import com.localproject.rental_local.repository.UserRepository;
import com.localproject.rental_local.service.ReviewService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    private ReviewService reviewService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        reviewService = new ReviewService(reviewRepository, userRepository, equipmentRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldSubmitReview() {
        User user = sampleUser(1L);
        Equipment equipment = sampleEquipment(10L);

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(equipmentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(equipment));
        when(reviewRepository.findByUserIdAndEquipmentId(1L, 10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(100L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        var result = reviewService.submitReview(new CreateReviewRequest(1L, 10L, 5, "Excellent item"));

        assertEquals(100L, result.id());
        assertEquals(5, result.rating());
        assertEquals(10L, result.equipmentId());
    }

    @Test
    void shouldRejectDuplicateReview() {
        User user = sampleUser(1L);
        Equipment equipment = sampleEquipment(10L);
        Review existing = Review.builder().id(99L).user(user).equipment(equipment).rating(4).comment("Old").build();

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(equipmentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(equipment));
        when(reviewRepository.findByUserIdAndEquipmentId(1L, 10L)).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.submitReview(new CreateReviewRequest(1L, 10L, 5, "Duplicate"))
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void shouldListReviewsByEquipment() {
        Equipment equipment = sampleEquipment(10L);
        Review review = Review.builder()
                .id(100L)
                .user(sampleUser(1L))
                .equipment(equipment)
                .rating(5)
                .comment("Great")
                .build();
        review.setCreatedAt(LocalDateTime.now());

        when(equipmentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(equipment));
        when(reviewRepository.findAllByEquipmentIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(review));

        var result = reviewService.listReviewsByEquipment(10L);

        assertEquals(1, result.size());
        assertEquals("Great", result.getFirst().comment());
    }

    @Test
    void shouldDeleteReview() {
        Review review = Review.builder()
                .id(100L)
                .user(sampleUser(1L))
                .equipment(sampleEquipment(10L))
                .rating(3)
                .comment("Okay")
                .build();

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(100L);

        verify(reviewRepository).delete(review);
    }

    private User sampleUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setEmail("user@test.com");
        user.setName("User");
        user.setPassword("hashed");
        return user;
    }

    private Equipment sampleEquipment(Long equipmentId) {
        Equipment equipment = new Equipment();
        equipment.setId(equipmentId);
        equipment.setName("Test Equipment");
        equipment.setCategory(EquipmentCategory.ELECTRONIC);
        equipment.setDailyRate(new BigDecimal("100.00"));
        equipment.setIsAvailable(true);
        equipment.setIsDeleted(false);
        equipment.setSeller(sampleUser(2L));
        return equipment;
    }
}

