package com.heardYou.checkin;

import com.heardYou.checkin.CheckinRequest.Duration;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.common.ValidationException;
import com.heardYou.warning.WarningSignService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Validates life-stage validation and allowed values per spec.
 */
@ExtendWith(MockitoExtension.class)
class CheckinLifeStageValidationTest {

    @Mock private ConditionSymptomRepository conditionSymptomRepository;
    @Mock private WarningSignService warningSignService;
    @InjectMocks private CheckinMatchingService service;

    @ParameterizedTest
    @ValueSource(strings = {"teen", "reproductive-age", "perimenopause", "postmenopause", "adult"})
    void allowedLifeStages_doNotThrow(String stage) {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList())).thenReturn(List.of());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinRequest req = new CheckinRequest();
        req.setLifeStage(stage);
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug("pelvic-pain");
        e.setSeverity(3);
        req.setSymptoms(List.of(e));

        assertThatNoException().isThrownBy(() -> service.match(req));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown-stage", "ADULT", "TEEN", "middle-age", "child", "elderly"})
    void invalidLifeStage_throwsValidationException(String stage) {
        CheckinRequest req = new CheckinRequest();
        req.setLifeStage(stage);
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug("pelvic-pain");
        e.setSeverity(3);
        req.setSymptoms(List.of(e));

        assertThatThrownBy(() -> service.match(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid lifeStage")
                .hasMessageContaining(stage);
    }

    @Test
    void responseContainsAlwaysOnSafetyLine() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList())).thenReturn(List.of());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinRequest req = new CheckinRequest();
        req.setLifeStage("adult");
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug("fatigue");
        e.setSeverity(3);
        req.setSymptoms(List.of(e));

        CheckinResponse response = service.match(req);

        assertThat(response.alwaysOnSafety()).isNotNull();
        assertThat(response.alwaysOnSafety()).contains("Don't wait for this app");
        assertThat(response.alwaysOnSafety()).contains("Seek care");
    }

    @Test
    void alwaysOnSafetyIsPresentEvenWithMatches() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList())).thenReturn(List.of());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinRequest req = new CheckinRequest();
        req.setLifeStage("reproductive-age");
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug("pelvic-pain");
        e.setSeverity(4);
        req.setSymptoms(List.of(e));

        CheckinResponse response = service.match(req);
        assertThat(response.alwaysOnSafety()).isEqualTo(CheckinResponse.ALWAYS_ON_SAFETY);
    }

    @Test
    void durationFromString_spec1_TO_4_WEEKS_mappedCorrectly() {
        assertThat(Duration.fromString("1_TO_4_WEEKS")).isEqualTo(Duration.ONE_TO_4_WEEKS);
        assertThat(Duration.fromString("ONE_TO_4_WEEKS")).isEqualTo(Duration.ONE_TO_4_WEEKS);
        assertThat(Duration.fromString("1_to_4_weeks")).isEqualTo(Duration.ONE_TO_4_WEEKS);
    }

    @Test
    void noMatchGuidanceIsNeverSuppressed() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList())).thenReturn(List.of());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinRequest req = new CheckinRequest();
        req.setLifeStage("adult");
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug("unknown-xyz");
        e.setSeverity(3);
        req.setSymptoms(List.of(e));

        CheckinResponse response = service.match(req);
        // No match → noMatchGuidance is populated and never reassuring
        assertThat(response.noMatchGuidance()).isNotNull();
        assertThat(response.noMatchGuidance()).doesNotContain("fine");
        assertThat(response.noMatchGuidance()).doesNotContain("safe");
        assertThat(response.noMatchGuidance()).doesNotContain("nothing to worry");
    }
}
