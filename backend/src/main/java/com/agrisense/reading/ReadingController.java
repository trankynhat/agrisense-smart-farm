package com.agrisense.reading;

import com.agrisense.security.Ownership;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sensors/{sensorId}/readings")
public class ReadingController {

    private final ReadingRepository readings;
    private final Ownership ownership;

    // FR-5.4: khoảng thời gian cho phép
    private static final Map<String, Duration> RANGES = Map.of(
            "1h", Duration.ofHours(1),
            "24h", Duration.ofHours(24),
            "7d", Duration.ofDays(7));

    public ReadingController(ReadingRepository readings, Ownership ownership) {
        this.readings = readings;
        this.ownership = ownership;
    }

    public record ReadingResponse(double value, OffsetDateTime recordedAt) {
        static ReadingResponse of(Reading r) {
            return new ReadingResponse(r.getValue(), r.getRecordedAt());
        }
    }

    @GetMapping
    public List<ReadingResponse> history(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sensorId,
            @RequestParam(defaultValue = "24h") String range) {
        ownership.sensor(userId, sensorId);
        Duration window = RANGES.get(range);
        if (window == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range phải là 1h | 24h | 7d");
        }
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minus(window);
        return readings.findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(sensorId, since)
                .stream().map(ReadingResponse::of).toList();
    }
}
