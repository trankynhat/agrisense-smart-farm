package com.agrisense.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    // alert gần đây của các sensor thuộc 1 farm (join qua sensor_id)
    List<Alert> findBySensorIdInOrderByCreatedAtDesc(List<Long> sensorIds);
}
