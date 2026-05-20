package com.daisimao.event;

import com.daisimao.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TaskEventConsumer {

    private static final String STREAM_KEY = "stream:task";
    private static final String GROUP_NAME = "notification-group";
    private static final String CONSUMER_NAME = "consumer-1";

    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    public TaskEventConsumer(RedisTemplate<String, String> redisTemplate,
                             NotificationService notificationService) {
        this.redisTemplate = redisTemplate;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void initConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
            log.info("Consumer group created: {}", GROUP_NAME);
        } catch (RedisSystemException e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("Consumer group already exists: {}", GROUP_NAME);
            } else {
                log.warn("Failed to create consumer group (Redis may be down): {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Scheduled(fixedDelay = 1000)
    public void poll() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(Consumer.from(GROUP_NAME, CONSUMER_NAME),
                          StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                          StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                Map<Object, Object> rawFields = record.getValue();
                Long taskId = Long.valueOf(String.valueOf(rawFields.get("taskId")));
                String type = String.valueOf(rawFields.get("type"));

                try {
                    notificationService.createFromEvent(taskId, type);
                } catch (Exception e) {
                    log.error("Failed to process event, will retry: taskId={}, type={}, error={}",
                            taskId, type, e.getMessage());
                    continue;
                }

                try {
                    redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME,
                            record.getId().getValue());
                    log.debug("Acknowledged event: type={}, taskId={}", type, taskId);
                } catch (Exception e) {
                    log.warn("Failed to acknowledge event, will retry: taskId={}, type={}", taskId, type);
                }
            }
        } catch (Exception e) {
            log.warn("Consumer poll cycle failed (Redis may be down): {}", e.getMessage());
        }
    }
}
