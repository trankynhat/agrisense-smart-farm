package com.agrisense.farm;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "farms")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "crop_type")
    private String cropType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Farm() {}

    public Farm(Long userId, String name, String location, String cropType) {
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.cropType = cropType;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getCropType() { return cropType; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
