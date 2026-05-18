package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daisimao.event.EventPublisher;
import com.daisimao.event.TaskEvent;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.PageResponse;
import com.daisimao.model.dto.TaskCreateRequest;
import com.daisimao.model.dto.TaskResponse;
import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import com.daisimao.repository.TaskRepository;
import com.daisimao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ContentFilterService contentFilterService;
    private final EventPublisher eventPublisher;

    private static final Set<Integer> ACTIVE_STATUSES = Set.of(2, 3); // ACCEPTED, IN_PROGRESS
    private static final int MAX_ACTIVE_TASKS = 3;
    private static final int MIN_CREDIT_SCORE = 60;

    @Transactional
    public TaskResponse createTask(Long publisherId, TaskCreateRequest request) {
        String checkText = request.getTitle();
        if (request.getDescription() != null) {
            checkText += " " + request.getDescription();
        }
        Set<String> hits = contentFilterService.match(checkText);
        if (!hits.isEmpty()) {
            throw new BusinessException("内容包含违规信息");
        }

        if (request.getReward().compareTo(new BigDecimal("20")) > 0
                || request.getReward().compareTo(new BigDecimal("1")) < 0) {
            throw new BusinessException("跑腿费需在 1-20 元之间");
        }

        Task task = new Task();
        task.setPublisherId(publisherId);
        task.setType(request.getType());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        task.setReward(request.getReward());
        task.setPickupLocation(request.getPickupLocation());
        task.setDeliveryLocation(request.getDeliveryLocation());
        task.setStatus(1);

        if (request.getDeadline() != null && !request.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(request.getDeadline()));
            } catch (Exception e) {
                throw new BusinessException("截止时间格式不正确");
            }
        }

        taskRepository.insert(task);
        eventPublisher.publish(new TaskEvent(task.getId(), "created"));
        log.info("Task created: id={}, publisher={}, type={}", task.getId(), publisherId, task.getType());
        return TaskResponse.from(task);
    }

    public PageResponse<TaskResponse> listTasks(Integer type, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, 1); // only PENDING
        if (type != null) {
            wrapper.eq(Task::getType, type);
        }
        wrapper.orderByDesc(Task::getCreatedAt);

        Page<Task> taskPage = taskRepository.selectPage(new Page<>(page, size), wrapper);
        List<Task> tasks = taskPage.getRecords();

        Map<Long, User> userMap = getUserMap(tasks);

        List<TaskResponse> items = tasks.stream()
                .map(t -> TaskResponse.from(t, userMap.get(t.getPublisherId()), null))
                .collect(Collectors.toList());

        return new PageResponse<>(items, taskPage.getTotal(), page, size);
    }

    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        User publisher = userRepository.selectById(task.getPublisherId());
        User acceptor = task.getAcceptorId() != null ? userRepository.selectById(task.getAcceptorId()) : null;
        return TaskResponse.from(task, publisher, acceptor);
    }

    private Map<Long, User> getUserMap(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> publisherIds = tasks.stream().map(Task::getPublisherId).collect(Collectors.toSet());
        return userRepository.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Transactional
    public TaskResponse handleAction(Long taskId, String action, Long userId) {
        return switch (action) {
            case "accept" -> acceptTask(taskId, userId);
            default -> throw new BusinessException("不支持的操作: " + action);
        };
    }

    private TaskResponse acceptTask(Long taskId, Long userId) {
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (task.getStatus() != 1) {
            throw new BusinessException("该任务已被接单或已取消");
        }
        if (task.getPublisherId().equals(userId)) {
            throw new BusinessException("不能接自己的任务");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("任务已过期");
        }

        User user = userRepository.selectById(userId);
        if (user == null || user.getCreditScore() < MIN_CREDIT_SCORE) {
            throw new BusinessException("信用分低于 " + MIN_CREDIT_SCORE + "，无法接单");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException("账号状态异常，无法接单");
        }

        long activeCount = taskRepository.countActiveTasksForUpdate(userId);
        if (activeCount >= MAX_ACTIVE_TASKS) {
            throw new BusinessException("同时进行中的任务已达上限（" + MAX_ACTIVE_TASKS + "个）");
        }

        task.setAcceptorId(userId);
        task.setStatus(2);
        task.setAcceptedAt(LocalDateTime.now());
        int updated = taskRepository.updateById(task);
        if (updated == 0) {
            throw new BusinessException("抢单失败，请重试");
        }

        eventPublisher.publish(new TaskEvent(task.getId(), "accepted"));
        log.info("Task accepted: id={}, acceptor={}", task.getId(), userId);

        User publisher = userRepository.selectById(task.getPublisherId());
        return TaskResponse.from(task, publisher, user);
    }
}
