-- Schema theo SRS §5. 5 bảng: users, farms, sensors, readings, alerts.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE farms (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    location   VARCHAR(255),
    crop_type  VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_farms_user_id ON farms(user_id);

CREATE TABLE sensors (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    farm_id       BIGINT       NOT NULL REFERENCES farms(id) ON DELETE CASCADE,
    type          VARCHAR(64)  NOT NULL,   -- soil_moisture, temperature, humidity, ph, light
    unit          VARCHAR(32)  NOT NULL,
    threshold_min DOUBLE PRECISION,
    threshold_max DOUBLE PRECISION
);
CREATE INDEX idx_sensors_farm_id ON sensors(farm_id);

CREATE TABLE readings (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sensor_id   BIGINT           NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    value       DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ      NOT NULL
);
-- index theo (sensor_id, recorded_at) cho query lịch sử — SRS §5
CREATE INDEX idx_readings_sensor_time ON readings(sensor_id, recorded_at DESC);

CREATE TABLE alerts (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sensor_id     BIGINT           NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
    reading_value DOUBLE PRECISION NOT NULL,
    threshold     DOUBLE PRECISION NOT NULL,
    type          VARCHAR(8)       NOT NULL,   -- min | max
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    resolved      BOOLEAN          NOT NULL DEFAULT false
);
CREATE INDEX idx_alerts_sensor_id ON alerts(sensor_id);
