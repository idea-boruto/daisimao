package com.daisimao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daisimao.exception.BusinessException;
import com.daisimao.model.dto.NotificationResponse;
import com.daisimao.model.entity.Notification;
import com.daisimao.model.entity.Task;
import com.daisimao.repository.NotificationRepository;
import com.daisimao.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks NotificationService notificationService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(10L);
        task.setPublisherId(1L);
        task.setAcceptorId(2L);
        task.setTitle("帮拿快递");
        task.setStatus(5);
        task.setReward(new BigDecimal("5"));
    }

    @Nested
    class CreateFromEvent {

        @Test
        void shouldCreateNotificationForAcceptedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "accepted");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            Notification n = captor.getValue();
            assertThat(n.getUserId()).isEqualTo(1L);
            assertThat(n.getType()).isEqualTo("accepted");
            assertThat(n.getContent()).contains("帮拿快递");
            assertThat(n.getRelatedTaskId()).isEqualTo(10L);
            assertThat(n.getIsRead()).isEqualTo(0);
        }

        @Test
        void shouldCreateNotificationForStartedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "started");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getType()).isEqualTo("started");
        }

        @Test
        void shouldCreateNotificationForCompletedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "completed");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getType()).isEqualTo("completed");
        }

        @Test
        void shouldCreateNotificationForConfirmedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "confirmed");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(2L);
            assertThat(captor.getValue().getType()).isEqualTo("confirmed");
        }

        @Test
        void shouldNotifyBothPartiesForCancelledEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "cancelled");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(2)).insert(captor.capture());
            List<Notification> all = captor.getAllValues();
            assertThat(all).extracting(Notification::getUserId).containsExactlyInAnyOrder(1L, 2L);
            assertThat(all).extracting(Notification::getType).containsOnly("cancelled");
        }

        @Test
        void shouldNotifyAcceptorForAutoReleasedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "auto_released");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(2L);
            assertThat(captor.getValue().getType()).isEqualTo("auto_released");
        }

        @Test
        void shouldNotifyBothPartiesForAutoConfirmedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "auto_confirmed");

            verify(notificationRepository, times(2)).insert(any(Notification.class));
        }

        @Test
        void shouldNotCreateNotificationForCreatedEvent() {
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "created");

            verify(notificationRepository, never()).insert(any());
        }

        @Test
        void shouldSkipWhenTaskNotFound() {
            when(taskRepository.selectById(99L)).thenReturn(null);

            notificationService.createFromEvent(99L, "accepted");

            verify(notificationRepository, never()).insert(any());
        }

        @Test
        void shouldBeIdempotentOnDuplicate() {
            when(taskRepository.selectById(10L)).thenReturn(task);
            when(notificationRepository.insert(any(Notification.class)))
                    .thenThrow(new DuplicateKeyException("duplicate"));

            assertThatCode(() -> notificationService.createFromEvent(10L, "accepted"))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldHandleCancelledWithNoAcceptor() {
            task.setAcceptorId(null);
            when(taskRepository.selectById(10L)).thenReturn(task);

            notificationService.createFromEvent(10L, "cancelled");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        }
    }

    @Nested
    class CreateDirect {

        @Test
        void shouldInsertNotificationWithCorrectFields() {
            notificationService.createDirect(3L, "review_new",
                    "你收到了新评价", "有人评价了你的任务", 10L);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).insert(captor.capture());
            Notification n = captor.getValue();
            assertThat(n.getUserId()).isEqualTo(3L);
            assertThat(n.getType()).isEqualTo("review_new");
            assertThat(n.getTitle()).isEqualTo("你收到了新评价");
            assertThat(n.getContent()).isEqualTo("有人评价了你的任务");
            assertThat(n.getRelatedTaskId()).isEqualTo(10L);
            assertThat(n.getIsRead()).isEqualTo(0);
        }

        @Test
        void shouldBeIdempotentOnDuplicate() {
            when(notificationRepository.insert(any(Notification.class)))
                    .thenThrow(new DuplicateKeyException("duplicate"));

            assertThatCode(() -> notificationService.createDirect(3L, "review_new",
                    "title", "content", 10L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class GetUserNotifications {

        @Test
        void shouldReturnNotificationsOrderedByCreatedAt() {
            Notification n1 = new Notification();
            n1.setId(1L);
            n1.setUserId(1L);
            n1.setType("accepted");
            n1.setTitle("t1");
            n1.setContent("c1");
            n1.setIsRead(0);

            when(notificationRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(n1));

            List<NotificationResponse> results = notificationService.getUserNotifications(1L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getType()).isEqualTo("accepted");
            assertThat(results.get(0).getIsRead()).isFalse();
        }

        @Test
        void shouldReturnEmptyListWhenNone() {
            when(notificationRepository.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<NotificationResponse> results = notificationService.getUserNotifications(1L);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    class MarkAsRead {

        @Test
        void shouldMarkAsRead() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(1L);
            n.setIsRead(0);

            when(notificationRepository.selectById(1L)).thenReturn(n);

            notificationService.markAsRead(1L, 1L);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).updateById(captor.capture());
            assertThat(captor.getValue().getIsRead()).isEqualTo(1);
        }

        @Test
        void shouldThrowWhenNotificationNotFound() {
            when(notificationRepository.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(404);
        }

        @Test
        void shouldThrowWhenNotOwnNotification() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(2L);

            when(notificationRepository.selectById(1L)).thenReturn(n);

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(403);
        }

        @Test
        void shouldNotUpdateIfAlreadyRead() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(1L);
            n.setIsRead(1);

            when(notificationRepository.selectById(1L)).thenReturn(n);

            notificationService.markAsRead(1L, 1L);

            verify(notificationRepository, never()).updateById(any());
        }
    }

    @Nested
    class GetUnreadCount {

        @Test
        void shouldReturnCount() {
            when(notificationRepository.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(5L);

            long count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        void shouldReturnZero() {
            when(notificationRepository.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L);

            long count = notificationService.getUnreadCount(1L);

            assertThat(count).isEqualTo(0L);
        }
    }
}
