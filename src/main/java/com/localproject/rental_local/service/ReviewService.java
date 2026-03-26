package com.localproject.rental_local.service;

import com.localproject.rental_local.dto.request.CreateReviewRequest;
import com.localproject.rental_local.dto.response.ReviewDto;
import com.localproject.rental_local.entity.Equipment;
import com.localproject.rental_local.entity.Review;
import com.localproject.rental_local.entity.User;
import com.localproject.rental_local.repository.EquipmentRepository;
import com.localproject.rental_local.repository.ReviewRepository;
import com.localproject.rental_local.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;

    public ReviewDto submitReview(CreateReviewRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(request.equipmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        reviewRepository.findByUserIdAndEquipmentId(user.getId(), equipment.getId())
                .ifPresent(review -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this user and equipment");
                });

        Review review = Review.builder()
                .user(user)
                .equipment(equipment)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return toDto(reviewRepository.save(review));
    }

    public List<ReviewDto> listReviewsByEquipment(Long equipmentId) {
        equipmentRepository.findByIdAndIsDeletedFalse(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        return reviewRepository.findAllByEquipmentIdOrderByCreatedAtDesc(equipmentId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<ReviewDto> listAllReviews() {
        return reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toDto)
                .toList();
    }

    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        reviewRepository.delete(review);
    }

    private ReviewDto toDto(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getUser().getId(),
                review.getEquipment().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
