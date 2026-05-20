package com.daisimao.event;

import com.daisimao.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskEventConsumerTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock StreamOperations<String, Object, Object> streamOperations;
    @Mock NotificationService notificationService;

    @InjectMocks TaskEventConsumer consumer;

    @SuppressWarnings("unchecked")
    private MapRecord<String, Object, Object> mockRecord(String taskId, String type) {
        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        RecordId recordId = RecordId.of(taskId + "-0");
        when(record.getId()).thenReturn(recordId);
        when(record.getValue()).thenReturn(Map.of("taskId", taskId, "type", type));
        return record;
    }

    @Test
    void shouldProcessBatchAndAcknowledge() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        MapRecord<String, Object, Object> record1 = mockRecord("10", "accepted");
        MapRecord<String, Object, Object> record2 = mockRecord("20", "completed");

        when(streamOperations.read(
                any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record1, record2));

        consumer.poll();

        verify(notificationService).createFromEvent(10L, "accepted");
        verify(notificationService).createFromEvent(20L, "completed");
        verify(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("10-0"));
        verify(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("20-0"));
    }

    @Test
    void shouldNotAcknowledgeFailedMessage() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        MapRecord<String, Object, Object> record1 = mockRecord("10", "accepted");
        MapRecord<String, Object, Object> record2 = mockRecord("20", "completed");

        when(streamOperations.read(
                any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record1, record2));

        doThrow(new RuntimeException("DB error"))
                .when(notificationService).createFromEvent(20L, "completed");
        doNothing()
                .when(notificationService).createFromEvent(10L, "accepted");

        consumer.poll();

        verify(notificationService).createFromEvent(10L, "accepted");
        verify(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("10-0"));
        verify(streamOperations, never()).acknowledge(eq("stream:task"), eq("notification-group"), eq("20-0"));
    }

    @Test
    void shouldSkipWhenNoMessages() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.read(
                any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(Collections.emptyList());

        consumer.poll();

        verify(notificationService, never()).createFromEvent(anyLong(), anyString());
        verify(streamOperations, never()).acknowledge(anyString(), anyString(), anyString());
    }

    @Test
    void shouldSurviveRedisException() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.read(
                any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        consumer.poll();

        verify(notificationService, never()).createFromEvent(anyLong(), anyString());
    }

    @Test
    void shouldContinueProcessingAfterAckFailure() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        MapRecord<String, Object, Object> record1 = mockRecord("10", "accepted");
        MapRecord<String, Object, Object> record2 = mockRecord("20", "completed");

        when(streamOperations.read(
                any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record1, record2));

        doThrow(new RuntimeException("Redis connection lost"))
                .when(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("10-0"));

        consumer.poll();

        verify(notificationService).createFromEvent(10L, "accepted");
        verify(notificationService).createFromEvent(20L, "completed");
        verify(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("10-0"));
        verify(streamOperations).acknowledge(eq("stream:task"), eq("notification-group"), eq("20-0"));
    }
}
