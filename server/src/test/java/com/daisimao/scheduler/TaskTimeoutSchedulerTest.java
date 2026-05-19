package com.daisimao.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.event.EventPublisher;
import com.daisimao.model.entity.Task;
import com.daisimao.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskTimeoutSchedulerTest {

    @Mock TaskRepository taskRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks TaskTimeoutScheduler scheduler;

    @Test
    void shouldReleaseExpiredAcceptedTasks() {
        Task expiredTask = new Task();
        expiredTask.setId(1L);
        expiredTask.setStatus(2);
        expiredTask.setAcceptedAt(LocalDateTime.now().minusHours(1));

        when(taskRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(expiredTask));
        when(taskRepository.updateById(any(Task.class))).thenReturn(1);

        scheduler.scheduleReleaseExpiredAcceptedTasks();

        verify(taskRepository).updateById(argThat(t -> t.getStatus() == 1 && t.getAcceptorId() == null));
        verify(eventPublisher).publish(argThat(e -> "auto_released".equals(e.getType())));
    }

    @Test
    void shouldNotFailWhenNoExpiredTasks() {
        when(taskRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        scheduler.scheduleReleaseExpiredAcceptedTasks();

        verify(taskRepository, never()).updateById(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldAutoConfirmExpiredPendingConfirmTasks() {
        Task expiredTask = new Task();
        expiredTask.setId(2L);
        expiredTask.setStatus(4);
        expiredTask.setCompletedAt(LocalDateTime.now().minusHours(25));

        when(taskRepository.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(expiredTask));
        when(taskRepository.updateById(any(Task.class))).thenReturn(1);

        scheduler.scheduleAutoConfirmPendingTasks();

        verify(taskRepository).updateById(argThat(t -> t.getStatus() == 5));
        verify(eventPublisher).publish(argThat(e -> "auto_confirmed".equals(e.getType())));
    }
}
