package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daisimao.event.EventPublisher;
import com.daisimao.event.TaskEvent;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.PageResponse;
import com.daisimao.model.dto.TaskCreateRequest;
import com.daisimao.model.dto.TaskResponse;
import com.daisimao.model.entity.CreditLog;
import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import com.daisimao.repository.CreditLogRepository;
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
    private final CreditLogRepository creditLogRepository;
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
                LocalDateTime deadline = LocalDateTime.parse(request.getDeadline());
                if (!deadline.isAfter(LocalDateTime.now())) {
                    throw new BusinessException("截止时间必须是将来的时间");
                }
                task.setDeadline(deadline);
            } catch (Exception e) {
                if (e instanceof BusinessException) throw (BusinessException) e;
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

    public List<TaskResponse> getMyPublishedTasks(Long userId) {
        List<Task> tasks = taskRepository.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getPublisherId, userId)
                        .orderByDesc(Task::getCreatedAt));
        Map<Long, User> userMap = loadUserMapFromTasks(tasks);
        return tasks.stream()
                .map(t -> TaskResponse.from(t, userMap.get(t.getPublisherId()),
                        t.getAcceptorId() != null ? userMap.get(t.getAcceptorId()) : null))
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getMyAcceptedTasks(Long userId) {
        List<Task> tasks = taskRepository.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getAcceptorId, userId)
                        .orderByDesc(Task::getCreatedAt));
        Map<Long, User> userMap = loadUserMapFromTasks(tasks);
        return tasks.stream()
                .map(t -> TaskResponse.from(t, userMap.get(t.getPublisherId()),
                        t.getAcceptorId() != null ? userMap.get(t.getAcceptorId()) : null))
                .collect(Collectors.toList());
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

    private Map<Long, User> loadUserMapFromTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = new HashSet<>();
        for (Task t : tasks) {
            ids.add(t.getPublisherId());
            if (t.getAcceptorId() != null) ids.add(t.getAcceptorId());
        }
        return userRepository.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Transactional
    public TaskResponse handleAction(Long taskId, String action, Long userId) {
        return switch (action) {
            case "accept"   -> acceptTask(taskId, userId);
            case "start"    -> startTask(taskId, userId);
            case "complete" -> completeTask(taskId, userId);
            case "confirm"  -> confirmTask(taskId, userId);
            case "cancel"   -> cancelTask(taskId, userId);
            default -> throw new BusinessException("不支持的操作: " + action);
        };
    }

    private Task loadTaskOrThrow(Long taskId) {
        Task task = taskRepository.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private TaskResponse acceptTask(Long taskId, Long userId) {
        Task task = loadTaskOrThrow(taskId);
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

    private TaskResponse startTask(Long taskId, Long userId) {
        Task task = loadTaskOrThrow(taskId);
        if (task.getStatus() != 2) {
            throw new BusinessException("任务状态不正确，无法确认开始");
        }
        if (!userId.equals(task.getAcceptorId())) {
            throw new BusinessException("只有接单人才能确认开始");
        }
        task.setStatus(3);
        int updated = taskRepository.updateById(task);
        if (updated == 0) {
            throw new BusinessException("操作失败，请重试");
        }
        eventPublisher.publish(new TaskEvent(task.getId(), "started"));
        log.info("Task started: id={}, acceptor={}", task.getId(), userId);
        User publisher = userRepository.selectById(task.getPublisherId());
        User acceptor = userRepository.selectById(userId);
        return TaskResponse.from(task, publisher, acceptor);
    }

    private TaskResponse completeTask(Long taskId, Long userId) {
        Task task = loadTaskOrThrow(taskId);
        if (task.getStatus() != 3) {
            throw new BusinessException("任务状态不正确，无法标记完成");
        }
        if (!userId.equals(task.getAcceptorId())) {
            throw new BusinessException("只有接单人才能标记完成");
        }
        task.setStatus(4);
        task.setCompletedAt(LocalDateTime.now());
        int updated = taskRepository.updateById(task);
        if (updated == 0) {
            throw new BusinessException("操作失败，请重试");
        }
        eventPublisher.publish(new TaskEvent(task.getId(), "completed"));
        log.info("Task completed: id={}, acceptor={}", task.getId(), userId);
        User publisher = userRepository.selectById(task.getPublisherId());
        User acceptor = userRepository.selectById(userId);
        return TaskResponse.from(task, publisher, acceptor);
    }

    private TaskResponse confirmTask(Long taskId, Long userId) {
        Task task = loadTaskOrThrow(taskId);
        if (task.getStatus() != 4) {
            throw new BusinessException("任务状态不正确，无法确认");
        }
        if (!userId.equals(task.getPublisherId())) {
            throw new BusinessException("只有发单人才能确认");
        }
        task.setStatus(5);
        int updated = taskRepository.updateById(task);
        if (updated == 0) {
            throw new BusinessException("操作失败，请重试");
        }
        User publisher = userRepository.selectById(userId);
        publisher.setCompletedOrders(publisher.getCompletedOrders() + 1);
        int userUpdated = userRepository.updateById(publisher);
        if (userUpdated == 0) {
            throw new BusinessException("操作失败，请重试");
        }
        eventPublisher.publish(new TaskEvent(task.getId(), "confirmed"));
        log.info("Task confirmed: id={}, publisher={}", task.getId(), userId);
        User acceptor = task.getAcceptorId() != null ? userRepository.selectById(task.getAcceptorId()) : null;
        return TaskResponse.from(task, publisher, acceptor);
    }

    private TaskResponse cancelTask(Long taskId, Long userId) {
        Task task = loadTaskOrThrow(taskId);
        boolean isPublisher = userId.equals(task.getPublisherId());
        boolean isAcceptor = userId.equals(task.getAcceptorId());
        if (!isPublisher && !isAcceptor) {
            throw new BusinessException("无权取消该任务");
        }
        int status = task.getStatus();
        if (status != 1 && status != 2 && status != 3) {
            throw new BusinessException("当前状态不允许取消");
        }

        int creditPenalty = 0;
        User penalizedUser = userRepository.selectById(userId);

        if (isPublisher && status == 1) {
            // self-cancel from PENDING: no credit penalty
        } else if (isPublisher) {
            creditPenalty = -3;
        } else {
            creditPenalty = -5;
        }

        task.setStatus(6);
        int updated = taskRepository.updateById(task);
        if (updated == 0) {
            throw new BusinessException("操作失败，请重试");
        }

        if (creditPenalty != 0) {
            penalizedUser.setCreditScore(penalizedUser.getCreditScore() + creditPenalty);
            penalizedUser.setCancelledOrders(penalizedUser.getCancelledOrders() + 1);
            insertCreditLog(penalizedUser.getId(), creditPenalty, "取消任务", task.getId());
            int userUpdated = userRepository.updateById(penalizedUser);
            if (userUpdated == 0) {
                throw new BusinessException("操作失败，请重试");
            }
        }

        eventPublisher.publish(new TaskEvent(task.getId(), "cancelled"));
        log.info("Task cancelled: id={}, by={}, penalty={}", task.getId(), userId, creditPenalty);

        User publisher = userRepository.selectById(task.getPublisherId());
        User acceptor = task.getAcceptorId() != null ? userRepository.selectById(task.getAcceptorId()) : null;
        return TaskResponse.from(task, publisher, acceptor);
    }

    private void insertCreditLog(Long userId, int changeAmount, String reason, Long taskId) {
        CreditLog log = new CreditLog();
        log.setUserId(userId);
        log.setChangeAmount(changeAmount);
        log.setReason(reason);
        log.setRelatedTaskId(taskId);
        creditLogRepository.insert(log);
    }
}
