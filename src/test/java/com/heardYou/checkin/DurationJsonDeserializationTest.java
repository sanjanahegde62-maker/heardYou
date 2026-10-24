package com.heardYou.checkin;

import tools.jackson.databind.ObjectMapper;
import com.heardYou.checkin.CheckinRequest.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that the Duration enum deserializes correctly through Jackson — the exact path
 * used by Spring MVC when processing a POST /api/checkin request body.
 *
 * Root cause of the bug: Jackson's default enum deserialization calls Enum.valueOf(),
 * which maps by Java constant name only.  "1_TO_4_WEEKS" is not a valid Java identifier,
 * so the constant was named ONE_TO_4_WEEKS and "1_TO_4_WEEKS" from the client was
 * rejected with 400/500.  Fix: @JsonCreator on fromString() makes Jackson call that
 * method instead, which handles the alias correctly.
 */
class DurationJsonDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ── deserialization (client → server) ────────────────────────────────────

    @ParameterizedTest(name = "\"{0}\" deserializes to {1}")
    @CsvSource({
        "UNDER_1_WEEK,   UNDER_1_WEEK",
        "1_TO_4_WEEKS,   ONE_TO_4_WEEKS",
        "OVER_1_MONTH,   OVER_1_MONTH",
        // enum constant name must also be accepted for internal round-trip
        "ONE_TO_4_WEEKS, ONE_TO_4_WEEKS",
        // case-insensitive
        "under_1_week,   UNDER_1_WEEK",
        "over_1_month,   OVER_1_MONTH"
    })
    void validDurationValuesDeserializeCorrectly(String jsonValue, String expectedConstant)
            throws Exception {
        String json = "{\"slug\":\"pelvic-pain\",\"severity\":3,\"duration\":\"" + jsonValue + "\"}";
        CheckinRequest.SymptomEntry entry = mapper.readValue(json, CheckinRequest.SymptomEntry.class);
        assertThat(entry.getDuration()).isEqualTo(Duration.valueOf(expectedConstant.trim()));
    }

    @Test
    void spec_1_TO_4_WEEKS_deserializes_to_ONE_TO_4_WEEKS_constant() throws Exception {
        // This was the exact broken case: "1_TO_4_WEEKS" came in from the client and
        // Jackson threw an UnrecognizedPropertyException / InvalidFormatException because
        // there is no enum constant literally named "1_TO_4_WEEKS".
        String json = "{\"slug\":\"fatigue\",\"severity\":4,\"duration\":\"1_TO_4_WEEKS\"}";
        CheckinRequest.SymptomEntry entry = mapper.readValue(json, CheckinRequest.SymptomEntry.class);
        assertThat(entry.getDuration()).isEqualTo(Duration.ONE_TO_4_WEEKS);
        assertThat(entry.getDuration().factor()).isEqualTo(0.8);
    }

    @Test
    void nullDuration_deserializesAsNull() throws Exception {
        String json = "{\"slug\":\"fatigue\",\"severity\":4}";
        CheckinRequest.SymptomEntry entry = mapper.readValue(json, CheckinRequest.SymptomEntry.class);
        assertThat(entry.getDuration()).isNull();
    }

    @Test
    void invalidDurationValue_throwsException() throws Exception {
        String json = "{\"slug\":\"fatigue\",\"severity\":3,\"duration\":\"3 days\"}";
        assertThatThrownBy(() -> mapper.readValue(json, CheckinRequest.SymptomEntry.class))
                .hasMessageContaining("Invalid duration value");
    }

    @Test
    void anotherInvalidValue_throwsException() throws Exception {
        String json = "{\"slug\":\"fatigue\",\"severity\":3,\"duration\":\"MONTHLY\"}";
        assertThatThrownBy(() -> mapper.readValue(json, CheckinRequest.SymptomEntry.class))
                .hasMessageContaining("Invalid duration value");
    }

    // ── serialization (server → client) ─────────────────────────────────────

    @Test
    void ONE_TO_4_WEEKS_serializesAs_1_TO_4_WEEKS() throws Exception {
        // Round-trip: the spec value "1_TO_4_WEEKS" must be what clients see in responses
        String serialized = mapper.writeValueAsString(Duration.ONE_TO_4_WEEKS);
        assertThat(serialized).isEqualTo("\"1_TO_4_WEEKS\"");
    }

    @Test
    void UNDER_1_WEEK_serializesAsSpecName() throws Exception {
        assertThat(mapper.writeValueAsString(Duration.UNDER_1_WEEK)).isEqualTo("\"UNDER_1_WEEK\"");
    }

    @Test
    void OVER_1_MONTH_serializesAsSpecName() throws Exception {
        assertThat(mapper.writeValueAsString(Duration.OVER_1_MONTH)).isEqualTo("\"OVER_1_MONTH\"");
    }

    // ── full request body round-trip ─────────────────────────────────────────

    @Test
    void fullCheckinRequest_withAllThreeDurationValues_deserializesCorrectly() throws Exception {
        String json = """
            {
              "lifeStage": "reproductive-age",
              "symptoms": [
                {"slug": "pelvic-pain",    "severity": 4, "duration": "OVER_1_MONTH"},
                {"slug": "fatigue",        "severity": 3, "duration": "1_TO_4_WEEKS"},
                {"slug": "heavy-periods",  "severity": 5, "duration": "UNDER_1_WEEK"}
              ]
            }
            """;
        CheckinRequest req = mapper.readValue(json, CheckinRequest.class);

        assertThat(req.getLifeStage()).isEqualTo("reproductive-age");
        assertThat(req.getSymptoms()).hasSize(3);
        assertThat(req.getSymptoms().get(0).getDuration()).isEqualTo(Duration.OVER_1_MONTH);
        assertThat(req.getSymptoms().get(1).getDuration()).isEqualTo(Duration.ONE_TO_4_WEEKS);
        assertThat(req.getSymptoms().get(2).getDuration()).isEqualTo(Duration.UNDER_1_WEEK);
    }
}
