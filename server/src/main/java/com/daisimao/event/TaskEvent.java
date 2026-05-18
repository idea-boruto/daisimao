package com.daisimao.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskEvent {
    private Long taskId;
    private String type; // created, accepted, completed, cancelled
}
