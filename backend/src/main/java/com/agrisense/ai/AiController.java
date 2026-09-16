package com.agrisense.ai;

import com.agrisense.alert.Alert;
import com.agrisense.alert.AlertRepository;
import com.agrisense.farm.Farm;
import com.agrisense.reading.Reading;
import com.agrisense.reading.ReadingRepository;
import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import com.agrisense.security.Ownership;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AiController {

    private final AiService ai;
    private final SensorRepository sensors;
    private final ReadingRepository readings;
    private final AlertRepository alerts;
    private final Ownership ownership;

    public AiController(AiService ai, SensorRepository sensors, ReadingRepository readings,
                        AlertRepository alerts, Ownership ownership) {
        this.ai = ai;
        this.sensors = sensors;
        this.readings = readings;
        this.alerts = alerts;
        this.ownership = ownership;
    }

    public record AiResponse(String text) {}

    // FR-7: gom readings 24h của farm → phân tích sức khỏe
    @PostMapping("/farms/{farmId}/analyze")
    public AiResponse analyze(@AuthenticationPrincipal Long userId, @PathVariable Long farmId) {
        Farm farm = ownership.farm(userId, farmId);
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);

        List<AiService.ReadingPoint> points = new ArrayList<>();
        for (Sensor s : sensors.findByFarmId(farmId)) {
            for (Reading r : readings.findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(s.getId(), since)) {
                points.add(new AiService.ReadingPoint(s.getType(), r.getValue(), r.getRecordedAt().toString()));
            }
        }
        return new AiResponse(ai.analyzeHealth(farm.getCropType(), points));
    }

    // FR-8: giải thích 1 alert
    @PostMapping("/alerts/{alertId}/explain")
    public AiResponse explain(@AuthenticationPrincipal Long userId, @PathVariable Long alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> ownership.notFound("alert"));
        Sensor sensor = ownership.sensor(userId, alert.getSensorId());
        return new AiResponse(ai.explainAlert(
                sensor.getType(), alert.getReadingValue(), alert.getThreshold(), alert.getType()));
    }
}
