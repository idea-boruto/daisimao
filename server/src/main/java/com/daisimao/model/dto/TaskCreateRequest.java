package com.daisimao.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TaskCreateRequest {

    @NotNull(message = "任务类型不能为空")
    @Min(value = 1, message = "任务类型无效")
    @Max(value = 5, message = "任务类型无效")
    private Integer type;

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 2, max = 128, message = "标题长度 2-128 位")
    private String title;

    @Size(max = 512, message = "描述最多 512 字")
    private String description;

    @NotNull(message = "跑腿费不能为空")
    @DecimalMin(value = "1.0", message = "跑腿费最少 1 元")
    @DecimalMax(value = "20.0", message = "跑腿费最多 20 元")
    private BigDecimal reward;

    @Size(max = 256, message = "取件地点最多 256 字")
    private String pickupLocation;

    @Size(max = 256, message = "送达地点最多 256 字")
    private String deliveryLocation;

    private String deadline;
}
