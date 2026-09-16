package com.agrisense.farm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/farms")
public class FarmController {

    private final FarmRepository farms;

    public FarmController(FarmRepository farms) {
        this.farms = farms;
    }

    public record CreateFarmRequest(
            @NotBlank String name, String location, String cropType) {}

    public record FarmResponse(
            Long id, String name, String location, String cropType, OffsetDateTime createdAt) {
        static FarmResponse of(Farm f) {
            return new FarmResponse(f.getId(), f.getName(), f.getLocation(),
                    f.getCropType(), f.getCreatedAt());
        }
    }

    // userId đến từ JwtAuthFilter (principal = userId)
    @GetMapping
    public List<FarmResponse> list(@AuthenticationPrincipal Long userId) {
        return farms.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(FarmResponse::of).toList();
    }

    @PostMapping
    public ResponseEntity<FarmResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateFarmRequest req) {
        Farm saved = farms.save(new Farm(userId, req.name(), req.location(), req.cropType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(FarmResponse.of(saved));
    }
}
