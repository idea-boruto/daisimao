package com.daisimao.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.event.EventPublisher;
import com.daisimao.event.TaskEvent;
import com.daisimao.model.entity.Task;
import com.daisimao.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTimeoutScheduler {

    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    @Scheduled(fixedDelay = 300_000)
    public void scheduleReleaseExpiredAcceptedTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, 2)
               .lt(Task::getAcceptedAt, cutoff);

        List<Task> expiredTasks = taskRepository.selectList(wrapper);
        if (expiredTasks.isEmpty()) return;

        int released = 0;
        for (Task task : expiredTasks) {
            task.setStatus(1);
            task.setAcceptorId(null);
            task.setAcceptedAt(null);
            int updated = taskRepository.updateById(task);
            if (updated == 0) {
                log.error("Auto-release version conflict: task id={}", task.getId());
                continue;
            }
            eventPublisher.publish(new TaskEvent(task.getId(), "auto_released"));
            log.info("Auto-released task: id={}", task.getId());
            released++;
        }
        log.info("Auto-release processed {}/{} tasks", released, expiredTasks.size());
    }

    @Transactional
    @Scheduled(fixedDelay = 600_000)
    public void scheduleAutoConfirmPendingTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, 4)
               .lt(Task::getCompletedAt, cutoff);

        List<Task> expiredTasks = taskRepository.selectList(wrapper);
        if (expiredTasks.isEmpty()) return;

        int confirmed = 0;
        for (Task task : expiredTasks) {
            task.setStatus(5);
            int updated = taskRepository.updateById(task);
            if (updated == 0) {
                log.error("Auto-confirm version conflict: task id={}", task.getId());
                continue;
            }
            eventPublisher.publish(new TaskEvent(task.getId(), "auto_confirmed"));
            log.info("Auto-confirmed task: id={}", task.getId());
            confirmed++;
        }
        log.info("Auto-confirm processed {}/{} tasks", confirmed, expiredTasks.size());
    }
}
