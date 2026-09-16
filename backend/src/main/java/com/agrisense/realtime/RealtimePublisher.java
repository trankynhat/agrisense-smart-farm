package com.agrisense.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Đẩy reading/alert tới subscriber STOMP của 1 farm: /topic/farm/{farmId}. */
@Component
public class RealtimePublisher {

    private final SimpMessagingTemplate messaging;

    public RealtimePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    // kind = "reading" | "alert" để client phân loại trên cùng 1 topic
    public record Envelope(String kind, Object payload) {}

    public record ReadingMsg(Long sensorId, String type, double value, OffsetDateTime recordedAt) {}

    public record AlertMsg(Long id, Long sensorId, String type, double value,
                           double threshold, OffsetDateTime createdAt) {}

    public void reading(Long farmId, ReadingMsg msg) {
        messaging.convertAndSend("/topic/farm/" + farmId, new Envelope("reading", msg));
    }

    public void alert(Long farmId, AlertMsg msg) {
        messaging.convertAndSend("/topic/farm/" + farmId, new Envelope("alert", msg));
    }
}
