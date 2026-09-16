package com.agrisense.demo;

import com.agrisense.farm.Farm;
import com.agrisense.farm.FarmRepository;
import com.agrisense.sensor.Sensor;
import com.agrisense.sensor.SensorRepository;
import com.agrisense.user.User;
import com.agrisense.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seed 1 tài khoản demo + farm + 5 sensor (NFR-5: link luôn có data).
 * Idempotent: chỉ seed nếu user demo chưa tồn tại.
 * Login: demo@agrisense.dev / demo1234 — sau đó chạy replay để thấy data live.
 */
@Component
public class DemoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final String DEMO_EMAIL = "demo@agrisense.dev";

    private final UserRepository users;
    private final FarmRepository farms;
    private final SensorRepository sensors;
    private final PasswordEncoder encoder;

    public DemoSeeder(UserRepository users, FarmRepository farms,
                      SensorRepository sensors, PasswordEncoder encoder) {
        this.users = users;
        this.farms = farms;
        this.sensors = sensors;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(DEMO_EMAIL)) return;

        User demo = users.save(new User(DEMO_EMAIL, encoder.encode("demo1234")));
        Farm farm = farms.save(new Farm(demo.getId(), "Nông trại Đà Lạt", "Đà Lạt", "Cà chua"));

        // ngưỡng khớp seed CSV (soil dip xuống ~24 < 30 → sinh alert khi replay)
        sensors.saveAll(List.of(
                new Sensor(farm.getId(), "soil_moisture", "%", 30.0, 90.0),
                new Sensor(farm.getId(), "temperature", "°C", 10.0, 38.0),
                new Sensor(farm.getId(), "humidity", "%", 40.0, 95.0),
                new Sensor(farm.getId(), "ph", "pH", 5.5, 7.5),
                new Sensor(farm.getId(), "light", "lux", 0.0, 100000.0)));

        log.info("Seeded demo: {} / demo1234, farm={}", DEMO_EMAIL, farm.getId());
    }
}
