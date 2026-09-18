package com.agrisense.stream;

import com.agrisense.alert.AlertService;
import com.agrisense.notification.TelegramNotificationService;
import com.agrisense.reading.Reading;
import com.agrisense.reading.ReadingRepository;
import com.agrisense.realtime.RealtimePublisher;
import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Consumer group listener: reading từ stream → lưu Postgres → check ngưỡng → push realtime → ack.
 * At-least-once: chỉ ack sau khi xử lý xong (fail → giữ pending, xử lý lại).
 */
@Component
public class ReadingConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(ReadingConsumer.class);

    private final ReadingRepository readings;
    private final SensorRepository sensors;
    private final AlertService alertService;
    private final RealtimePublisher realtime;
    private final StringRedisTemplate redis;
    private final TelegramNotificationService telegram;

    public ReadingConsumer(ReadingRepository readings, SensorRepository sensors,
                           AlertService alertService, RealtimePublisher realtime,
                           StringRedisTemplate redis, TelegramNotificationService telegram) {
        this.readings = readings;
        this.sensors = sensors;
        this.alertService = alertService;
        this.realtime = realtime;
        this.redis = redis;
        this.telegram = telegram;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        Map<String, String> body = record.getValue();
        try {
            long sensorId = Long.parseLong(body.get("sensorId"));
            double value = Double.parseDouble(body.get("value"));
            OffsetDateTime recordedAt = OffsetDateTime.parse(body.get("recordedAt"));

            readings.save(new Reading(sensorId, value, recordedAt));

            Sensor sensor = sensors.findById(sensorId).orElse(null);
            if (sensor != null) {
                Long farmId = sensor.getFarmId();
                realtime.reading(farmId, new RealtimePublisher.ReadingMsg(
                        sensorId, sensor.getType(), value, recordedAt));

                alertService.checkAndCreate(sensor, value).ifPresent(a -> {
                    OffsetDateTime created = a.getCreatedAt() != null ? a.getCreatedAt() : recordedAt;
                    realtime.alert(farmId, new RealtimePublisher.AlertMsg(
                            a.getId(), sensorId, a.getType(), value, a.getThreshold(), created));
                    telegram.notifyWarning(a, sensor, farmId, recordedAt);
                });
            }

            redis.opsForStream().acknowledge(StreamConstants.GROUP, record);
        } catch (Exception e) {
            // không ack → message ở pending, claim + retry sau
            log.warn("Bỏ qua reading lỗi id={}: {}", record.getId(), e.getMessage());
        }
    }
}
