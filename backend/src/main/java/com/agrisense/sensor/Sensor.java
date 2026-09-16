package com.agrisense.sensor;

import jakarta.persistence.*;

@Entity
@Table(name = "sensors")
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(nullable = false)
    private String type;   // soil_moisture, temperature, humidity, ph, light

    @Column(nullable = false)
    private String unit;

    @Column(name = "threshold_min")
    private Double thresholdMin;

    @Column(name = "threshold_max")
    private Double thresholdMax;

    protected Sensor() {}

    public Sensor(Long farmId, String type, String unit, Double thresholdMin, Double thresholdMax) {
        this.farmId = farmId;
        this.type = type;
        this.unit = unit;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
    }

    public Long getId() { return id; }
    public Long getFarmId() { return farmId; }
    public String getType() { return type; }
    public String getUnit() { return unit; }
    public Double getThresholdMin() { return thresholdMin; }
    public Double getThresholdMax() { return thresholdMax; }
}
