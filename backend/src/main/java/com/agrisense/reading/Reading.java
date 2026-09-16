package com.agrisense.reading;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "readings")
public class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(nullable = false)
    private double value;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    protected Reading() {}

    public Reading(Long sensorId, double value, OffsetDateTime recordedAt) {
        this.sensorId = sensorId;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public Long getSensorId() { return sensorId; }
    public double getValue() { return value; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
}
