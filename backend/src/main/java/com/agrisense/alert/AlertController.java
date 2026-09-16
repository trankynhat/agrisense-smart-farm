package com.agrisense.alert;

import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import com.agrisense.security.Ownership;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
public class AlertController {

    private final AlertRepository alerts;
    private final SensorRepository sensors;
    private final Ownership ownership;

    public AlertController(AlertRepository alerts, SensorRepository sensors, Ownership ownership) {
        this.alerts = alerts;
        this.sensors = sensors;
        this.ownership = ownership;
    }

    public record AlertResponse(Long id, Long sensorId, double readingValue, double threshold,
                                String type, OffsetDateTime createdAt, boolean resolved) {
        static AlertResponse of(Alert a) {
            return new AlertResponse(a.getId(), a.getSensorId(), a.getReadingValue(),
                    a.getThreshold(), a.getType(), a.getCreatedAt(), a.isResolved());
        }
    }

    // FR-6.3: alert gần đây của 1 farm
    @GetMapping("/farms/{farmId}/alerts")
    public List<AlertResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long farmId) {
        ownership.farm(userId, farmId);
        List<Long> sensorIds = sensors.findByFarmId(farmId).stream().map(Sensor::getId).toList();
        if (sensorIds.isEmpty()) return List.of();
        return alerts.findBySensorIdInOrderByCreatedAtDesc(sensorIds)
                .stream().map(AlertResponse::of).toList();
    }

    // FR-6.3: đánh dấu đã xử lý
    @PostMapping("/alerts/{alertId}/resolve")
    public AlertResponse resolve(@AuthenticationPrincipal Long userId, @PathVariable Long alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> ownership.notFound("alert"));
        ownership.sensor(userId, alert.getSensorId()); // xác nhận sở hữu
        alert.resolve();
        return AlertResponse.of(alerts.save(alert));
    }
}
