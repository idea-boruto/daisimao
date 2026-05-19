package com.daisimao.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String studentId;
    private String campus;

    @TableField(value = "credit_score")
    private Integer creditScore;

    private Integer completedOrders;
    private Integer cancelledOrders;
    private Integer disputeOrders;
    private Integer role;
    private Integer status;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
