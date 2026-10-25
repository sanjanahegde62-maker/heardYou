package com.heardYou.checkin;

import com.heardYou.brief.BriefRequest;
import com.heardYou.condition.ConditionRepository;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.config.DataLoader;
import com.heardYou.source.MedicalSourceRepository;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSignRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring-configured ObjectMapper (with JacksonConfig applied)
 * correctly deserializes the application's private-field request DTOs.
 *
 * This test loads the full Spring context (mocking JPA and DataSource to avoid
 * needing a DB) and auto-wires the ObjectMapper that Spring MVC uses for real
 * requests. It ensures the Jackson 3 field-visibility fix in JacksonConfig is active.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class RequestDtoDeserializationTest {

    // Mocking DataSource prevents DataSourceConfig from attempting a real connection.
    @MockitoBean DataSource dataSource;
    // Mocking DataLoader prevents it from running ApplicationRunner.run() on startup.
    @MockitoBean DataLoader dataLoader;

    @MockitoBean ConditionRepository conditionRepository;
    @MockitoBean ConditionSymptomRepository conditionSymptomRepository;
    @MockitoBean SymptomRepository symptomRepository;
    @MockitoBean WarningSignRepository warningSignRepository;
    @MockitoBean MedicalSourceRepository medicalSourceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void springObjectMapper_deserializesCheckinRequest_withPrivateFields() throws Exception {
        String json = """
                {
                  "lifeStage": "adult",
                  "symptoms": [
                    {"slug": "fatigue",     "severity": 3, "duration": "1_TO_4_WEEKS"},
                    {"slug": "pelvic-pain", "severity": 5, "duration": "OVER_1_MONTH"}
                  ]
                }
                """;
        CheckinRequest req = objectMapper.readValue(json, CheckinRequest.class);
        assertThat(req.getLifeStage()).isEqualTo("adult");
        assertThat(req.getSymptoms()).hasSize(2);
        assertThat(req.getSymptoms().get(0).getSlug()).isEqualTo("fatigue");
        assertThat(req.getSymptoms().get(0).getSeverity()).isEqualTo(3);
        assertThat(req.getSymptoms().get(0).getDuration())
                .isEqualTo(CheckinRequest.Duration.ONE_TO_4_WEEKS);
        assertThat(req.getSymptoms().get(1).getSlug()).isEqualTo("pelvic-pain");
        assertThat(req.getSymptoms().get(1).getDuration())
                .isEqualTo(CheckinRequest.Duration.OVER_1_MONTH);
    }

    @Test
    void springObjectMapper_deserializesCheckinRequest_emptySymptomArray() throws Exception {
        // This was the exact failing case: symptoms:[] triggered "Request body could not
        // be read: invalid format" in the running server (Jackson 3 visibility bug).
        // With JacksonConfig applied, this must deserialize without error (validation
        // will reject it later with size >= 1, but deserialization must succeed).
        String json = "{\"lifeStage\":\"adult\",\"symptoms\":[]}";
        CheckinRequest req = objectMapper.readValue(json, CheckinRequest.class);
        assertThat(req.getLifeStage()).isEqualTo("adult");
        assertThat(req.getSymptoms()).isNotNull().isEmpty();
    }

    @Test
    void springObjectMapper_deserializesBriefRequest_withPrivateFields() throws Exception {
        String json = """
                {
                  "lifeStage": "reproductive-age",
                  "displayName": "Test User",
                  "symptoms": [
                    {"slug": "pelvic-pain", "severity": 4, "duration": "OVER_1_MONTH"},
                    {"slug": "fatigue",     "severity": 3, "duration": "1_TO_4_WEEKS"}
                  ],
                  "selectedSlugs": ["endometriosis", "pcos"]
                }
                """;
        BriefRequest req = objectMapper.readValue(json, BriefRequest.class);
        assertThat(req.getLifeStage()).isEqualTo("reproductive-age");
        assertThat(req.getDisplayName()).isEqualTo("Test User");
        assertThat(req.getSymptoms()).hasSize(2);
        assertThat(req.getSymptoms().get(0).getSlug()).isEqualTo("pelvic-pain");
        assertThat(req.getSymptoms().get(0).getSeverity()).isEqualTo(4);
        assertThat(req.getSymptoms().get(0).getDuration()).isEqualTo("OVER_1_MONTH");
        assertThat(req.getSelectedSlugs()).containsExactly("endometriosis", "pcos");
    }

    @Test
    void springObjectMapper_deserializesCheckinRequest_nullDuration() throws Exception {
        String json = """
                {"lifeStage":"teen","symptoms":[{"slug":"headache","severity":2}]}
                """;
        CheckinRequest req = objectMapper.readValue(json, CheckinRequest.class);
        assertThat(req.getSymptoms().get(0).getDuration()).isNull();
    }
}