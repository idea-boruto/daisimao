package com.daisimao.model.dto;

import com.daisimao.model.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private Long relatedTaskId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.type = n.getType();
        r.title = n.getTitle();
        r.content = n.getContent();
        r.isRead = n.getIsRead() != null && n.getIsRead() == 1;
        r.relatedTaskId = n.getRelatedTaskId();
        r.createdAt = n.getCreatedAt();
        return r;
    }
}
