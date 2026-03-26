package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.request.CreateReviewRequest;
import com.localproject.rental_local.dto.response.ReviewDto;
import com.localproject.rental_local.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto submitReview(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.submitReview(request);
    }

    @GetMapping("/equipment/{id}")
    public List<ReviewDto> getEquipmentReviews(@PathVariable Long id) {
        return reviewService.listReviewsByEquipment(id);
    }

    @GetMapping("/")
    public List<ReviewDto> getAllReviews() {
        return reviewService.listAllReviews();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
    }
}
