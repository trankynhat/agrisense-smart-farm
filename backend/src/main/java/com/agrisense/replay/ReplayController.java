package com.agrisense.replay;

import com.agrisense.security.Ownership;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/farms/{farmId}/replay")
public class ReplayController {

    private final ReplayService replay;
    private final Ownership ownership;

    public ReplayController(ReplayService replay, Ownership ownership) {
        this.replay = replay;
        this.ownership = ownership;
    }

    /** Bắt đầu replay seed CSV vào stream. mode=historical|live. Trả 202 (chạy nền). */
    @PostMapping
    public ResponseEntity<Map<String, String>> start(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long farmId,
            @RequestParam(defaultValue = "historical") ReplayService.Mode mode,
            @RequestParam(defaultValue = "1000") long intervalMs) {
        ownership.farm(userId, farmId);
        replay.replay(farmId, mode, intervalMs);
        return ResponseEntity.accepted().body(Map.of(
                "status", "started", "farmId", String.valueOf(farmId), "mode", mode.name()));
    }
}
