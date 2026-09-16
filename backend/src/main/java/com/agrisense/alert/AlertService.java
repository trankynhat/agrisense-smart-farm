package com.agrisense.alert;

import com.agrisense.sensor.Sensor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Kiểm tra 1 reading có vượt ngưỡng sensor không; nếu có → tạo & lưu Alert. */
@Service
public class AlertService {

    private final AlertRepository alerts;

    public AlertService(AlertRepository alerts) {
        this.alerts = alerts;
    }

    /** @return Alert đã lưu nếu vượt ngưỡng, else empty. */
    public Optional<Alert> checkAndCreate(Sensor sensor, double value) {
        Double min = sensor.getThresholdMin();
        Double max = sensor.getThresholdMax();

        if (min != null && value < min) {
            return Optional.of(alerts.save(new Alert(sensor.getId(), value, min, "min")));
        }
        if (max != null && value > max) {
            return Optional.of(alerts.save(new Alert(sensor.getId(), value, max, "max")));
        }
        return Optional.empty();
    }
}
