package com.agrisense.stream;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

@Configuration
public class StreamConfig {

    private final StringRedisTemplate redis;

    @Value("${agrisense.stream.consumer-name:worker-1}")
    private String consumerName;

    public StreamConfig(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Tạo consumer group nếu chưa có (MKSTREAM tạo luôn stream). Idempotent. */
    @PostConstruct
    void ensureGroup() {
        try {
            redis.opsForStream().createGroup(StreamConstants.STREAM_KEY, StreamConstants.GROUP);
        } catch (Exception ignored) {
            // group đã tồn tại (BUSYGROUP) → bỏ qua
        }
    }

    @Bean(destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamContainer(
            RedisConnectionFactory factory, ReadingConsumer consumer) throws InterruptedException {

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory, options);

        // đọc message mới cho group (>) — at-least-once, ack thủ công trong consumer
        Subscription sub = container.receive(
                Consumer.from(StreamConstants.GROUP, consumerName),
                StreamOffset.create(StreamConstants.STREAM_KEY, ReadOffset.lastConsumed()),
                consumer);
        sub.await(Duration.ofSeconds(5));

        container.start();
        return container;
    }
}
