package com.agrisense.security;

import com.agrisense.farm.Farm;
import com.agrisense.farm.FarmRepository;
import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Kiểm tra farm/sensor thuộc về user (FR-2.2). Không thuộc → 404 (không lộ tồn tại). */
@Component
public class Ownership {

    private final FarmRepository farms;
    private final SensorRepository sensors;

    public Ownership(FarmRepository farms, SensorRepository sensors) {
        this.farms = farms;
        this.sensors = sensors;
    }

    public Farm farm(Long userId, Long farmId) {
        Farm farm = farms.findById(farmId)
                .orElseThrow(() -> notFound("farm"));
        if (!farm.getUserId().equals(userId)) throw notFound("farm");
        return farm;
    }

    public Sensor sensor(Long userId, Long sensorId) {
        Sensor sensor = sensors.findById(sensorId)
                .orElseThrow(() -> notFound("sensor"));
        farm(userId, sensor.getFarmId());  // xác nhận farm cha thuộc user
        return sensor;
    }

    public ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy " + what);
    }
}
