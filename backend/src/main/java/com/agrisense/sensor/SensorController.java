package com.agrisense.sensor;

import com.agrisense.security.Ownership;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/farms/{farmId}/sensors")
public class SensorController {

    private final SensorRepository sensors;
    private final Ownership ownership;

    public SensorController(SensorRepository sensors, Ownership ownership) {
        this.sensors = sensors;
        this.ownership = ownership;
    }

    public record CreateSensorRequest(
            @NotBlank String type, @NotBlank String unit,
            Double thresholdMin, Double thresholdMax) {}

    public record SensorResponse(
            Long id, Long farmId, String type, String unit,
            Double thresholdMin, Double thresholdMax) {
        static SensorResponse of(Sensor s) {
            return new SensorResponse(s.getId(), s.getFarmId(), s.getType(),
                    s.getUnit(), s.getThresholdMin(), s.getThresholdMax());
        }
    }

    @GetMapping
    public List<SensorResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long farmId) {
        ownership.farm(userId, farmId);
        return sensors.findByFarmId(farmId).stream().map(SensorResponse::of).toList();
    }

    @PostMapping
    public ResponseEntity<SensorResponse> create(
            @AuthenticationPrincipal Long userId, @PathVariable Long farmId,
            @Valid @RequestBody CreateSensorRequest req) {
        ownership.farm(userId, farmId);
        Sensor saved = sensors.save(new Sensor(
                farmId, req.type(), req.unit(), req.thresholdMin(), req.thresholdMax()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SensorResponse.of(saved));
    }
}
