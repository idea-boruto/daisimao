package com.daisimao.model.dto;

import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private Integer type;
    private String title;
    private String description;
    private BigDecimal reward;
    private String pickupLocation;
    private String deliveryLocation;
    private Integer status;
    private Long publisherId;
    private String publisherNickname;
    private Integer publisherCreditScore;
    private Long acceptorId;
    private String acceptorNickname;
    private LocalDateTime deadline;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static TaskResponse from(Task task) {
        return from(task, null, null);
    }

    public static TaskResponse from(Task task, User publisher, User acceptor) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.type = task.getType();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.reward = task.getReward();
        r.pickupLocation = task.getPickupLocation();
        r.deliveryLocation = task.getDeliveryLocation();
        r.status = task.getStatus();
        r.publisherId = task.getPublisherId();
        r.acceptorId = task.getAcceptorId();
        r.deadline = task.getDeadline();
        r.acceptedAt = task.getAcceptedAt();
        r.completedAt = task.getCompletedAt();
        r.createdAt = task.getCreatedAt();
        if (publisher != null) {
            r.publisherNickname = publisher.getNickname();
            r.publisherCreditScore = publisher.getCreditScore();
        }
        if (acceptor != null) {
            r.acceptorNickname = acceptor.getNickname();
        }
        return r;
    }
}
