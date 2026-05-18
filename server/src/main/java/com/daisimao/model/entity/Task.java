package com.daisimao.model.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private Integer type;
    private String title;
    private String description;
    private BigDecimal reward;
    private String pickupLocation;
    private String deliveryLocation;
    private Integer status;
    private LocalDateTime deadline;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
