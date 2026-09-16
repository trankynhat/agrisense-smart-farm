package com.agrisense.replay;

import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import com.agrisense.stream.ReadingPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replay dataset CSV vào Redis Stream.
 * CSV long-format, header: recorded_at,type,value  (recorded_at ISO-8601).
 * - historical: giữ recorded_at gốc, bơm liên tục (không sleep).
 * - live: recorded_at = now, sleep giữa các điểm để mô phỏng thời gian thực.
 */
@Service
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);
    private static final String SEED_CSV = "data/seed_readings.csv";

    private final SensorRepository sensors;
    private final ReadingPublisher publisher;

    public ReplayService(SensorRepository sensors, ReadingPublisher publisher) {
        this.sensors = sensors;
        this.publisher = publisher;
    }

    public enum Mode { historical, live }

    /** Chạy nền: publish mỗi row CSV khớp sensor của farm vào stream. */
    @Async
    public void replay(Long farmId, Mode mode, long liveIntervalMs) {
        // map type -> sensorId cho farm này (bỏ row không có sensor tương ứng)
        Map<String, Long> byType = new HashMap<>();
        for (Sensor s : sensors.findByFarmId(farmId)) {
            byType.putIfAbsent(s.getType(), s.getId());
        }
        if (byType.isEmpty()) {
            log.warn("Replay farm {}: chưa có sensor nào, bỏ qua", farmId);
            return;
        }

        int published = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource(SEED_CSV).getInputStream(), StandardCharsets.UTF_8))) {
            String line = r.readLine(); // header
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cols = List.of(line.split(",", 3));
                if (cols.size() < 3) continue;
                String type = cols.get(1).trim();
                Long sensorId = byType.get(type);
                if (sensorId == null) continue;

                double value = Double.parseDouble(cols.get(2).trim());
                String recordedAt = mode == Mode.live
                        ? OffsetDateTime.now(ZoneOffset.UTC).toString()
                        : cols.get(0).trim();
                publisher.publish(sensorId, value, recordedAt);
                published++;

                if (mode == Mode.live && liveIntervalMs > 0) {
                    Thread.sleep(liveIntervalMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Replay farm {} lỗi: {}", farmId, e.getMessage());
        }
        log.info("Replay farm {} ({}) xong: {} readings", farmId, mode, published);
    }
}
