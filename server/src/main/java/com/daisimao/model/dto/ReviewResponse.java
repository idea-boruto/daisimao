package com.daisimao.model.dto;

import com.daisimao.model.entity.Review;
import com.daisimao.model.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Long taskId;
    private Long reviewerId;
    private String reviewerNickname;
    private Long targetId;
    private String targetNickname;
    private Integer rating;
    private String tags;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review, User reviewer, User target) {
        ReviewResponse r = new ReviewResponse();
        r.id = review.getId();
        r.taskId = review.getTaskId();
        r.reviewerId = review.getReviewerId();
        r.targetId = review.getTargetId();
        r.rating = review.getRating();
        r.tags = review.getTags();
        r.comment = review.getComment();
        r.createdAt = review.getCreatedAt();
        if (reviewer != null) {
            r.reviewerNickname = reviewer.getNickname();
        }
        if (target != null) {
            r.targetNickname = target.getNickname();
        }
        return r;
    }
}
