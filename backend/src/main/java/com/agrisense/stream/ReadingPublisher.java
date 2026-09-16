package com.agrisense.stream;

import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Đẩy 1 reading vào Redis Stream. Nguồn: replay CSV hoặc live. */
@Component
public class ReadingPublisher {

    private final StringRedisTemplate redis;

    public ReadingPublisher(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void publish(long sensorId, double value, String recordedAtIso) {
        Map<String, String> fields = Map.of(
                "sensorId", String.valueOf(sensorId),
                "value", String.valueOf(value),
                "recordedAt", recordedAtIso);
        redis.opsForStream().add(StreamRecords.mapBacked(fields)
                .withStreamKey(StreamConstants.STREAM_KEY));
    }
}
