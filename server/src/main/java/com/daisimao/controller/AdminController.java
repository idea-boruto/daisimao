package com.daisimao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import com.daisimao.repository.TaskRepository;
import com.daisimao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        long totalUsers = userRepository.selectCount(null);
        long completedOrders = taskRepository.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, 5));
        long disputeOrders = taskRepository.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, 7));
        long todayOrders = taskRepository.selectCount(
                new LambdaQueryWrapper<Task>()
                        .ge(Task::getCreatedAt, LocalDate.now().atStartOfDay()));

        return ResponseEntity.ok(Map.of(
                "todayOrders", todayOrders,
                "completedOrders", completedOrders,
                "activeUsers", totalUsers,
                "disputeOrders", disputeOrders
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getNickname, keyword)
                   .or()
                   .like(User::getUsername, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        List<User> users = userRepository.selectList(wrapper);
        return ResponseEntity.ok(Map.of("items", users));
    }

    @PutMapping("/users/{id}/freeze")
    public ResponseEntity<Map<String, Object>> freezeUser(@PathVariable Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(0);
        userRepository.updateById(user);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/users/{id}/unfreeze")
    public ResponseEntity<Map<String, Object>> unfreezeUser(@PathVariable Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(1);
        userRepository.updateById(user);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        wrapper.orderByDesc(Task::getCreatedAt);
        List<Task> tasks = taskRepository.selectList(wrapper);
        return ResponseEntity.ok(Map.of("items", tasks));
    }

    @PutMapping("/tasks/{id}/force-cancel")
    public ResponseEntity<Map<String, Object>> forceCancel(@PathVariable Long id) {
        Task task = taskRepository.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (task.getStatus() == 5 || task.getStatus() == 6) {
            throw new BusinessException("该任务已结束，无法取消");
        }
        task.setStatus(6);
        taskRepository.updateById(task);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
