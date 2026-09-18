package com.agrisense.notification;

import com.agrisense.alert.Alert;
import com.agrisense.sensor.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Sends each threshold alert to a Telegram chat when Telegram is configured. */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx");

    private final RestClient http = RestClient.create();
    private final String botToken;
    private final String chatId;

    public TelegramNotificationService(
            @Value("${agrisense.telegram.bot-token:}") String botToken,
            @Value("${agrisense.telegram.chat-id:}") String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public boolean enabled() {
        return !botToken.isBlank() && !chatId.isBlank();
    }

    @Async
    public void notifyWarning(Alert alert, Sensor sensor, Long farmId, OffsetDateTime recordedAt) {
        if (!enabled()) return;

        try {
            http.post()
                    .uri("https://api.telegram.org/bot" + botToken + "/sendMessage")
                    .body(Map.of("chat_id", chatId, "text", formatWarning(alert, sensor, farmId, recordedAt)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Telegram failure must not block Redis acknowledgement or leak the bot token in logs.
            log.warn("Telegram notification failed for alert {}: {}", alert.getId(), e.getClass().getSimpleName());
        }
    }

    static String formatWarning(Alert alert, Sensor sensor, Long farmId, OffsetDateTime recordedAt) {
        String direction = "min".equals(alert.getType()) ? "THẤP HƠN" : "CAO HƠN";
        OffsetDateTime time = recordedAt != null ? recordedAt : alert.getCreatedAt();
        return "⚠️ AgriSense cảnh báo\n"
                + "Farm: " + farmId + "\n"
                + "Sensor: " + sensor.getType() + " (#" + sensor.getId() + ")\n"
                + "Giá trị: " + alert.getReadingValue() + " " + sensor.getUnit() + "\n"
                + "Ngưỡng: " + direction + " " + alert.getThreshold() + " " + sensor.getUnit() + "\n"
                + "Thời gian: " + (time == null ? "unknown" : TIME_FORMAT.format(time));
    }
}
