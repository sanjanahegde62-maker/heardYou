package com.heardYou.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HealthController.
 * HealthController has no dependencies, so a plain unit test is sufficient.
 */
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void health_returns200WithStatusUp() {
        var response = controller.health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("HeardYou API");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void health_timestampIsAnIso8601String() {
        var response = controller.health();

        String timestamp = (String) response.getBody().get("timestamp");
        // Instant.toString() produces ISO-8601, e.g. 2024-01-15T10:30:00.123456Z
        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
    }
}
