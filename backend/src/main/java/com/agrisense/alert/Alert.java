package com.agrisense.alert;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "reading_value", nullable = false)
    private double readingValue;

    @Column(nullable = false)
    private double threshold;

    @Column(nullable = false)
    private String type;   // min | max

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private boolean resolved = false;

    protected Alert() {}

    public Alert(Long sensorId, double readingValue, double threshold, String type) {
        this.sensorId = sensorId;
        this.readingValue = readingValue;
        this.threshold = threshold;
        this.type = type;
    }

    public Long getId() { return id; }
    public Long getSensorId() { return sensorId; }
    public double getReadingValue() { return readingValue; }
    public double getThreshold() { return threshold; }
    public String getType() { return type; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public boolean isResolved() { return resolved; }
    public void resolve() { this.resolved = true; }
}
