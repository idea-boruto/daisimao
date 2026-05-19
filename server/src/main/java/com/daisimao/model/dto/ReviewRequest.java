package com.daisimao.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分至少为1星")
    @Max(value = 5, message = "评分最多为5星")
    private Integer rating;

    private String tags;

    private String comment;
}
