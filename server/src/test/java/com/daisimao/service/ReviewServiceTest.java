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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock CreditService creditService;
    @Mock NotificationService notificationService;

    @InjectMocks ReviewService reviewService;

    private Task completedTask;
    private User publisher;
    private User acceptor;

    @BeforeEach
    void setUp() {
        publisher = new User();
        publisher.setId(1L);
        publisher.setNickname("小明");
        publisher.setCreditScore(100);
        publisher.setStatus(1);

        acceptor = new User();
        acceptor.setId(2L);
        acceptor.setNickname("小红");
        acceptor.setCreditScore(80);
        acceptor.setStatus(1);

        completedTask = new Task();
        completedTask.setId(10L);
        completedTask.setPublisherId(1L);
        completedTask.setAcceptorId(2L);
        completedTask.setStatus(5);
        completedTask.setTitle("帮拿快递");
        completedTask.setReward(new BigDecimal("5"));
    }

    private ReviewRequest buildRequest(int rating) {
        ReviewRequest req = new ReviewRequest();
        req.setTaskId(10L);
        req.setRating(rating);
        req.setComment("很靠谱");
        return req;
    }

    @Nested
    class SubmitReview {

        @Test
        void shouldSubmitReviewAsPublisher() {
            when(taskRepository.selectById(10L)).thenReturn(completedTask);
            doAnswer(inv -> { Review r = inv.getArgument(0); r.setId(1L); return 1; })
                    .when(reviewRepository).insert(any(Review.class));
            when(creditService.applyReviewScore(2L, 5, 10L)).thenReturn(85);
            when(userRepository.selectById(1L)).thenReturn(publisher);
            when(userRepository.selectById(2L)).thenReturn(acceptor);

            ReviewResponse result = reviewService.submitReview(1L, buildRequest(5));

            assertThat(result.getReviewerId()).isEqualTo(1L);
            assertThat(result.getTargetId()).isEqualTo(2L);
            assertThat(result.getRating()).isEqualTo(5);
            assertThat(result.getReviewerNickname()).isEqualTo("小明");
            assertThat(result.getTargetNickname()).isEqualTo("小红");
            verify(creditService).applyReviewScore(2L, 5, 10L);
            verify(notificationService).createDirect(2L, "review_new",
                    "你收到了新评价", "有人评价了你的任务「帮拿快递」", 10L);
        }

        @Test
        void shouldSubmitReviewAsAcceptor() {
            when(taskRepository.selectById(10L)).thenReturn(completedTask);
            doAnswer(inv -> { Review r = inv.getArgument(0); r.setId(2L); return 1; })
                    .when(reviewRepository).insert(any(Review.class));
            when(creditService.applyReviewScore(1L, 4, 10L)).thenReturn(103);
            when(userRepository.selectById(2L)).thenReturn(acceptor);
            when(userRepository.selectById(1L)).thenReturn(publisher);

            ReviewResponse result = reviewService.submitReview(2L, buildRequest(4));

            assertThat(result.getReviewerId()).isEqualTo(2L);
            assertThat(result.getTargetId()).isEqualTo(1L);
            verify(notificationService).createDirect(1L, "review_new",
                    "你收到了新评价", "有人评价了你的任务「帮拿快递」", 10L);
        }

        @Test
        void shouldRejectNonCompletedTask() {
            completedTask.setStatus(3);
            when(taskRepository.selectById(10L)).thenReturn(completedTask);

            assertThatThrownBy(() -> reviewService.submitReview(1L, buildRequest(5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已完成");
        }

        @Test
        void shouldRejectNonExistentTask() {
            when(taskRepository.selectById(99L)).thenReturn(null);
            ReviewRequest req = buildRequest(5);
            req.setTaskId(99L);

            assertThatThrownBy(() -> reviewService.submitReview(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
        }

        @Test
        void shouldRejectNonParticipant() {
            when(taskRepository.selectById(10L)).thenReturn(completedTask);

            assertThatThrownBy(() -> reviewService.submitReview(99L, buildRequest(5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未参与");
        }

        @Test
        void shouldRejectDuplicateReview() {
            when(taskRepository.selectById(10L)).thenReturn(completedTask);
            when(reviewRepository.insert(any(Review.class)))
                    .thenThrow(new DuplicateKeyException("duplicate"));

            assertThatThrownBy(() -> reviewService.submitReview(1L, buildRequest(5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已经评价过");
        }
    }

    @Nested
    class GetTaskReviews {

        @Test
        void shouldReturnReviewsWithUserInfo() {
            Review review = new Review();
            review.setId(1L);
            review.setTaskId(10L);
            review.setReviewerId(1L);
            review.setTargetId(2L);
            review.setRating(5);
            review.setComment("很好");

            when(reviewRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(review));
            when(userRepository.selectBatchIds(Set.of(1L, 2L)))
                    .thenReturn(List.of(publisher, acceptor));

            List<ReviewResponse> results = reviewService.getTaskReviews(10L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getReviewerNickname()).isEqualTo("小明");
            assertThat(results.get(0).getTargetNickname()).isEqualTo("小红");
        }

        @Test
        void shouldReturnEmptyWhenNoReviews() {
            when(reviewRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<ReviewResponse> results = reviewService.getTaskReviews(10L);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class GetUserReviews {

        @Test
        void shouldReturnReviewsForTargetUser() {
            Review review = new Review();
            review.setId(1L);
            review.setTaskId(10L);
            review.setReviewerId(1L);
            review.setTargetId(2L);
            review.setRating(5);

            when(reviewRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(review));
            when(userRepository.selectBatchIds(Set.of(1L, 2L)))
                    .thenReturn(List.of(publisher, acceptor));

            List<ReviewResponse> results = reviewService.getUserReviews(2L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTargetId()).isEqualTo(2L);
        }

        @Test
        void shouldReturnEmptyWhenNoReviews() {
            when(reviewRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<ReviewResponse> results = reviewService.getUserReviews(2L);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class HasReviewed {

        @Test
        void shouldReturnTrueWhenExists() {
            when(reviewRepository.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(1L);

            assertThat(reviewService.hasReviewed(10L, 1L)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNotExists() {
            when(reviewRepository.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);

            assertThat(reviewService.hasReviewed(10L, 1L)).isFalse();
        }
    }
}
