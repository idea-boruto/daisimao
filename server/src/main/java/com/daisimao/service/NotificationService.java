package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.NotificationResponse;
import com.daisimao.model.entity.Notification;
import com.daisimao.model.entity.Task;
import com.daisimao.repository.NotificationRepository;
import com.daisimao.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void createFromEvent(Long taskId, String type) {
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            log.warn("Task not found for event: taskId={}, type={}", taskId, type);
            return;
        }
        String title = task.getTitle();

        switch (type) {
            case "accepted" -> createOne(task.getPublisherId(), "accepted",
                    "有人接取了你的任务",
                    "有人接取了你的任务「" + title + "」", taskId);
            case "started" -> createOne(task.getPublisherId(), "started",
                    "你的任务已开始处理",
                    "你的任务「" + title + "」已开始处理", taskId);
            case "completed" -> createOne(task.getPublisherId(), "completed",
                    "你的任务已完成，请确认",
                    "你的任务「" + title + "」已完成，请确认", taskId);
            case "confirmed" -> {
                if (task.getAcceptorId() != null) {
                    createOne(task.getAcceptorId(), "confirmed",
                            "发单人已确认完成",
                            "发单人已确认完成「" + title + "」", taskId);
                }
            }
            case "cancelled" -> {
                createOne(task.getPublisherId(), "cancelled",
                        "任务已取消", "任务「" + title + "」已被取消", taskId);
                if (task.getAcceptorId() != null) {
                    createOne(task.getAcceptorId(), "cancelled",
                            "任务已取消", "任务「" + title + "」已被取消", taskId);
                }
            }
            case "auto_released" -> {
                if (task.getAcceptorId() != null) {
                    createOne(task.getAcceptorId(), "auto_released",
                            "任务已超时释放",
                            "你接取的任务「" + title + "」已超时自动释放", taskId);
                }
            }
            case "auto_confirmed" -> {
                createOne(task.getPublisherId(), "auto_confirmed",
                        "任务已自动确认",
                        "任务「" + title + "」已超时自动确认完成", taskId);
                if (task.getAcceptorId() != null) {
                    createOne(task.getAcceptorId(), "auto_confirmed",
                            "任务已自动确认",
                            "任务「" + title + "」已超时自动确认完成", taskId);
                }
            }
            default -> log.debug("Unknown event type ignored: {}", type);
        }
    }

    private void createOne(Long userId, String type, String title, String content, Long taskId) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(title);
            n.setContent(content);
            n.setIsRead(0);
            n.setRelatedTaskId(taskId);
            notificationRepository.insert(n);
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate notification ignored: userId={}, type={}, taskId={}", userId, type, taskId);
        }
    }

    public void createDirect(Long userId, String type, String title, String content, Long relatedTaskId) {
        createOne(userId, type, title, content, relatedTaskId);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt));
        return notifications.stream().map(NotificationResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.selectById(notificationId);
        if (n == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!n.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (n.getIsRead() == null || n.getIsRead() == 0) {
            n.setIsRead(1);
            notificationRepository.updateById(n);
        }
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }
}
