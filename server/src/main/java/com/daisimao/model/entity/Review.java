package com.daisimao.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long reviewerId;
    private Long targetId;
    private Integer rating;
    private String tags;
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
