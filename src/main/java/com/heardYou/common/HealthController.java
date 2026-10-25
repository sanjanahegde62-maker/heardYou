package com.heardYou.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    /**
     * Health check endpoint.
     *
     * <p>Exposed at two paths:
     * <ul>
     *   <li>{@code GET /api/health} — the canonical API path used by the frontend.</li>
     *   <li>{@code GET /health}     — bare path for Render's built-in health checker and
     *       any reverse-proxy that strips the {@code /api} prefix before forwarding.</li>
     * </ul>
     *
     * <p>Both paths return the same JSON body with HTTP 200.
     */
    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "HeardYou API",
                "timestamp", Instant.now().toString()));
    }
}
