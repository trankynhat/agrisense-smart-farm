package com.agrisense.notification;

import com.agrisense.alert.Alert;
import com.agrisense.sensor.Sensor;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramNotificationServiceTest {

    @Test
    void formatsWarningWithSensorContext() {
        Sensor sensor = new Sensor(7L, "soil_moisture", "%", 30.0, 90.0);
        Alert alert = new Alert(null, 24.5, 30.0, "min");
        String message = TelegramNotificationService.formatWarning(
                alert, sensor, 7L, OffsetDateTime.parse("2026-09-18T10:15:30+07:00"));

        assertAll(
                () -> assertTrue(message.contains("AgriSense cảnh báo")),
                () -> assertTrue(message.contains("soil_moisture")),
                () -> assertTrue(message.contains("24.5 %")),
                () -> assertTrue(message.contains("THẤP HƠN 30.0 %")),
                () -> assertTrue(message.contains("2026-09-18 10:15:30 +07:00")));
    }
}
