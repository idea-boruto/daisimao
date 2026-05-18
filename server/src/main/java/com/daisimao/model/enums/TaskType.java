package com.daisimao.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskType {
    EXPRESS(1, "快递代取"),
    MEAL(2, "食堂带饭"),
    PRINT(3, "代打印"),
    SHOPPING(4, "代购"),
    OTHER(5, "其他");

    @EnumValue
    private final int code;
    private final String label;

    TaskType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
