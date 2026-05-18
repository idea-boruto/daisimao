package com.daisimao.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class EventPublisher {

    private static final String STREAM_KEY = "stream:task";

    private final RedisTemplate<String, String> redisTemplate;

    public EventPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(TaskEvent event) {
        Map<String, String> fields = Map.of(
                "taskId", event.getTaskId().toString(),
                "type", event.getType(),
                "timestamp", Instant.now().toString()
        );
        redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                        .in(STREAM_KEY)
                        .ofMap(fields)
        );
        log.debug("Event published: {} -> {}", event.getType(), event.getTaskId());
    }
}
