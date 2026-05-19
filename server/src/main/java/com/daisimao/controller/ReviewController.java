package com.daisimao.controller;

import com.daisimao.model.dto.ReviewRequest;
import com.daisimao.model.dto.ReviewResponse;
import com.daisimao.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ReviewResponse response = reviewService.submitReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<ReviewResponse>> getTaskReviews(@PathVariable Long taskId) {
        return ResponseEntity.ok(reviewService.getTaskReviews(taskId));
    }

    @GetMapping("/about/{userId}")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId));
    }

    @GetMapping("/task/{taskId}/check")
    public ResponseEntity<Boolean> checkReviewed(
            @PathVariable Long taskId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(reviewService.hasReviewed(taskId, userId));
    }
}
