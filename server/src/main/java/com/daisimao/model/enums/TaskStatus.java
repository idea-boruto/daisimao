package com.daisimao.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING(1, "待接单"),
    ACCEPTED(2, "已接单"),
    IN_PROGRESS(3, "进行中"),
    PENDING_CONFIRM(4, "待确认"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    DISPUTE(7, "纠纷中");

    @EnumValue
    private final int code;
    private final String label;

    TaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
