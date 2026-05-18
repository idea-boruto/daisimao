package com.daisimao.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.daisimao.model.enums.TaskStatus;
import com.daisimao.model.enums.TaskType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publisherId;
    private Long acceptorId;
    private TaskType type;
    private String title;
    private String description;
    private BigDecimal reward;
    private String pickupLocation;
    private String deliveryLocation;
    private TaskStatus status;
    private LocalDateTime deadline;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
