package com.agrisense.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface ReadingRepository extends JpaRepository<Reading, Long> {
    // lịch sử theo sensor + khoảng thời gian (FR-5.4: 1h/24h/7d)
    List<Reading> findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(Long sensorId, OffsetDateTime since);
}
