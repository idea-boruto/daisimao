package com.daisimao.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String type;
    private String title;
    private String content;

    @TableField("is_read")
    private Integer isRead;

    private Long relatedTaskId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
