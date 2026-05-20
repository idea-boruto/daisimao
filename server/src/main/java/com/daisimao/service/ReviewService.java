package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.ReviewRequest;
import com.daisimao.model.dto.ReviewResponse;
import com.daisimao.model.entity.Review;
import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import com.daisimao.repository.ReviewRepository;
import com.daisimao.repository.TaskRepository;
import com.daisimao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse submitReview(Long reviewerId, ReviewRequest request) {
        Task task = taskRepository.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (task.getStatus() != 5) {
            throw new BusinessException("只有已完成的任务才能评价");
        }

        Long targetId;
        if (reviewerId.equals(task.getPublisherId())) {
            targetId = task.getAcceptorId();
        } else if (reviewerId.equals(task.getAcceptorId())) {
            targetId = task.getPublisherId();
        } else {
            throw new BusinessException("你未参与该任务，无法评价");
        }
        if (targetId == null) {
            throw new BusinessException("该任务没有对方参与，无法评价");
        }

        Review review = new Review();
        review.setTaskId(request.getTaskId());
        review.setReviewerId(reviewerId);
        review.setTargetId(targetId);
        review.setRating(request.getRating());
        review.setTags(request.getTags());
        review.setComment(request.getComment());

        try {
            reviewRepository.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("你已经评价过该任务了");
        }

        creditService.applyReviewScore(targetId, request.getRating(), request.getTaskId());
        log.info("Review submitted: taskId={}, reviewer={}, target={}, rating={}",
                request.getTaskId(), reviewerId, targetId, request.getRating());

        notificationService.createDirect(targetId, "review_new",
                "你收到了新评价",
                "有人评价了你的任务「" + task.getTitle() + "」",
                request.getTaskId());

        User reviewer = userRepository.selectById(reviewerId);
        User target = userRepository.selectById(targetId);
        return ReviewResponse.from(review, reviewer, target);
    }

    public List<ReviewResponse> getTaskReviews(Long taskId) {
        List<Review> reviews = reviewRepository.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getTaskId, taskId));
        Map<Long, User> userMap = loadUserMap(reviews);
        return reviews.stream()
                .map(r -> ReviewResponse.from(r, userMap.get(r.getReviewerId()), userMap.get(r.getTargetId())))
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getUserReviews(Long userId) {
        List<Review> reviews = reviewRepository.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetId, userId)
                        .orderByDesc(Review::getCreatedAt));
        Map<Long, User> userMap = loadUserMap(reviews);
        return reviews.stream()
                .map(r -> ReviewResponse.from(r, userMap.get(r.getReviewerId()), userMap.get(r.getTargetId())))
                .collect(Collectors.toList());
    }

    public boolean hasReviewed(Long taskId, Long reviewerId) {
        return reviewRepository.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTaskId, taskId)
                        .eq(Review::getReviewerId, reviewerId)) > 0;
    }

    private Map<Long, User> loadUserMap(List<Review> reviews) {
        Set<Long> ids = reviews.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getReviewerId(), r.getTargetId()))
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userRepository.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
