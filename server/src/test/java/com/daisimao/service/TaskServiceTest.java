package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daisimao.event.EventPublisher;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.PageResponse;
import com.daisimao.model.dto.TaskCreateRequest;
import com.daisimao.model.dto.TaskResponse;
import com.daisimao.model.entity.Task;
import com.daisimao.model.entity.User;
import com.daisimao.repository.TaskRepository;
import com.daisimao.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock ContentFilterService contentFilterService;
    @Mock EventPublisher eventPublisher;

    @InjectMocks TaskService taskService;

    private User publisher;
    private User acceptor;
    private Task pendingTask;

    @BeforeEach
    void setUp() {
        publisher = new User();
        publisher.setId(1L);
        publisher.setNickname("小明");
        publisher.setCreditScore(100);
        publisher.setStatus(1);

        acceptor = new User();
        acceptor.setId(2L);
        acceptor.setNickname("小红");
        acceptor.setCreditScore(80);
        acceptor.setStatus(1);

        pendingTask = new Task();
        pendingTask.setId(10L);
        pendingTask.setPublisherId(1L);
        pendingTask.setType(1);
        pendingTask.setTitle("帮拿快递");
        pendingTask.setReward(new BigDecimal("5"));
        pendingTask.setStatus(1);
        pendingTask.setCreatedAt(LocalDateTime.now().minusHours(1));
    }

    // ── listTasks ──

    @Test
    void listTasks_shouldReturnOnlyPendingTasks() {
        when(taskRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<Task>(1, 20).setRecords(List.of(pendingTask)));
        when(userRepository.selectBatchIds(Set.of(1L))).thenReturn(List.of(publisher));

        PageResponse<TaskResponse> result = taskService.listTasks(null, 1, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(10L);
        assertThat(result.getItems().get(0).getPublisherNickname()).isEqualTo("小明");
    }

    @Test
    void listTasks_shouldReturnEmptyWhenNoTasks() {
        when(taskRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<Task>(1, 20).setRecords(List.of()));

        PageResponse<TaskResponse> result = taskService.listTasks(null, 1, 20);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
    }

    // ── getTask ──

    @Test
    void getTask_shouldReturnTaskWithUserInfo() {
        when(taskRepository.selectById(10L)).thenReturn(pendingTask);
        when(userRepository.selectById(1L)).thenReturn(publisher);

        TaskResponse result = taskService.getTask(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPublisherNickname()).isEqualTo("小明");
        assertThat(result.getPublisherCreditScore()).isEqualTo(100);
    }

    @Test
    void getTask_shouldThrowWhenNotFound() {
        when(taskRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.getTask(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(404);
    }

    // ── acceptTask ──

    @Nested
    class AcceptTask {

        @Test
        void shouldAcceptTaskSuccessfully() {
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);
            when(userRepository.selectById(2L)).thenReturn(acceptor);
            when(taskRepository.countActiveTasksForUpdate(2L)).thenReturn(0L);
            when(taskRepository.updateById(any(Task.class))).thenReturn(1);
            when(userRepository.selectById(1L)).thenReturn(publisher);

            TaskResponse result = taskService.handleAction(10L, "accept", 2L);

            assertThat(result.getStatus()).isEqualTo(2);
            assertThat(result.getAcceptorId()).isEqualTo(2L);
            assertThat(result.getAcceptorNickname()).isEqualTo("小红");

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(2);
            assertThat(captor.getValue().getAcceptorId()).isEqualTo(2L);
            assertThat(captor.getValue().getAcceptedAt()).isNotNull();

            verify(eventPublisher).publish(argThat(e -> "accepted".equals(e.getType())));
        }

        @Test
        void shouldRejectOwnTask() {
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("自己的任务");
        }

        @Test
        void shouldRejectNonPendingTask() {
            pendingTask.setStatus(2); // already accepted
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已被接单");
        }

        @Test
        void shouldRejectWhenCreditScoreTooLow() {
            acceptor.setCreditScore(50);
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);
            when(userRepository.selectById(2L)).thenReturn(acceptor);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("信用分");
        }

        @Test
        void shouldRejectWhenTooManyActiveTasks() {
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);
            when(userRepository.selectById(2L)).thenReturn(acceptor);
            when(taskRepository.countActiveTasksForUpdate(2L)).thenReturn(3L);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("上限");
        }

        @Test
        void shouldRejectExpiredTask() {
            pendingTask.setDeadline(LocalDateTime.now().minusMinutes(1));
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("过期");
        }

        @Test
        void shouldRejectWhenUserDisabled() {
            acceptor.setStatus(0);
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);
            when(userRepository.selectById(2L)).thenReturn(acceptor);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("状态异常");
        }

        @Test
        void shouldRejectUnknownAction() {
            assertThatThrownBy(() -> taskService.handleAction(10L, "unknown", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持");
        }

        @Test
        void shouldHandleOptimisticLockFailure() {
            when(taskRepository.selectById(10L)).thenReturn(pendingTask);
            when(userRepository.selectById(2L)).thenReturn(acceptor);
            when(taskRepository.countActiveTasksForUpdate(2L)).thenReturn(0L);
            when(taskRepository.updateById(any(Task.class))).thenReturn(0);

            assertThatThrownBy(() -> taskService.handleAction(10L, "accept", 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("重试");
        }
    }

    // ── createTask (existing, ensure not broken) ──

    @Test
    void createTask_shouldStillWork() {
        when(contentFilterService.match(anyString())).thenReturn(Set.of());
        doAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return 1;
        }).when(taskRepository).insert(any(Task.class));

        TaskCreateRequest req = new TaskCreateRequest();
        req.setType(1);
        req.setTitle("帮拿快递");
        req.setReward(new BigDecimal("5"));

        TaskResponse result = taskService.createTask(1L, req);

        assertThat(result.getTitle()).isEqualTo("帮拿快递");
        assertThat(result.getStatus()).isEqualTo(1);
        verify(eventPublisher).publish(argThat(e -> "created".equals(e.getType())));
    }
}
