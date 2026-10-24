package com.heardYou.warning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WarningSignService — evaluates warning signs independently of condition scores.
 */
@ExtendWith(MockitoExtension.class)
class WarningSignServiceTest {

    @Mock
    private WarningSignRepository warningSignRepository;

    @InjectMocks
    private WarningSignService service;

    // ── helpers ───────────────────────────────────────────────────────────────

    private WarningSign makeWarning(String name, int minMatches, String requiredStage,
                                    String... triggerSlugs) {
        WarningSign w = new WarningSign();
        w.setName(name);
        w.setMessage("Seek care for " + name);
        w.setRecommendedAction("Act now");
        w.setTriggerSymptomSlugs(java.util.Arrays.asList(triggerSlugs));
        w.setMinMatches(minMatches);
        w.setRequiredLifeStage(requiredStage);
        w.setSourceUrl("https://example.com");
        w.setSourceTitle("NHS");
        return w;
    }

    // ── minMatches ────────────────────────────────────────────────────────────

    @Test
    void singleTriggerSlug_minMatches1_firesOnMatch() {
        WarningSign w = makeWarning("Heavy Bleeding", 1, null, "heavy-periods");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(w));

        List<WarningSign> result = service.evaluateForSymptoms(List.of("heavy-periods"), null);

        assertThat(result).containsExactly(w);
    }

    @Test
    void heartAttackPattern_minMatches3_requiresThreeOfSixTriggers() {
        WarningSign cardiac = makeWarning("Cardiac Emergency", 3, null,
                "chest-discomfort", "shortness-of-breath", "jaw-neck-pain", "nausea", "dizziness", "palpitations");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(cardiac));

        // Only 2 of 6 triggers submitted — should NOT fire
        List<WarningSign> twoTriggers = service.evaluateForSymptoms(
                List.of("chest-discomfort", "nausea"), null);
        assertThat(twoTriggers).isEmpty();

        // 3 of 6 triggers submitted — SHOULD fire
        List<WarningSign> threeTriggers = service.evaluateForSymptoms(
                List.of("chest-discomfort", "nausea", "dizziness"), null);
        assertThat(threeTriggers).containsExactly(cardiac);
    }

    @Test
    void lupusKidney_minMatches2_requiresTwoOfThree() {
        WarningSign lupus = makeWarning("Lupus Kidney", 2, null,
                "urinary-symptoms", "rash", "joint-pain");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(lupus));

        // Only 1 trigger — should NOT fire
        assertThat(service.evaluateForSymptoms(List.of("rash"), null)).isEmpty();

        // 2 triggers — SHOULD fire
        assertThat(service.evaluateForSymptoms(List.of("rash", "joint-pain"), null))
                .containsExactly(lupus);
    }

    // ── requiredLifeStage ─────────────────────────────────────────────────────

    @Test
    void noLifeStageRestriction_firesForAllStages() {
        WarningSign w = makeWarning("General Warning", 1, null, "pelvic-pain");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(w));

        assertThat(service.evaluateForSymptoms(List.of("pelvic-pain"), "teen")).hasSize(1);
        assertThat(service.evaluateForSymptoms(List.of("pelvic-pain"), "postmenopause")).hasSize(1);
        assertThat(service.evaluateForSymptoms(List.of("pelvic-pain"), null)).hasSize(1);
    }

    @Test
    void lifeStageRestricted_onlyFiresForMatchingStage() {
        WarningSign postpartum = makeWarning("Postpartum Warning", 1, "reproductive-age", "heavy-periods");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(postpartum));

        // Wrong stage → should NOT fire
        assertThat(service.evaluateForSymptoms(List.of("heavy-periods"), "postmenopause")).isEmpty();

        // Correct stage → SHOULD fire
        assertThat(service.evaluateForSymptoms(List.of("heavy-periods"), "reproductive-age"))
                .containsExactly(postpartum);
    }

    // ── independence from match scores ───────────────────────────────────────

    @Test
    void warningsFire_evenWhenNoConditionPatternMatches() {
        // This test documents that WarningSignService is completely independent of
        // the condition matching score — it has no dependency on ConditionSymptomRepository.
        WarningSign emergency = makeWarning("Emergency", 1, null, "chest-discomfort");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(emergency));

        List<WarningSign> result = service.evaluateForSymptoms(List.of("chest-discomfort"), "adult");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Emergency");
    }

    // ── empty inputs ─────────────────────────────────────────────────────────

    @Test
    void emptySlugs_returnsEmpty() {
        assertThat(service.evaluateForSymptoms(List.of(), null)).isEmpty();
        verifyNoInteractions(warningSignRepository);
    }

    @Test
    void nullSlugs_returnsEmpty() {
        assertThat(service.evaluateForSymptoms(null, null)).isEmpty();
        verifyNoInteractions(warningSignRepository);
    }

    // ── stroke and postpartum warning sign coverage ───────────────────────────

    @Test
    void strokeWarning_firesOnDizzinessAndHeadache() {
        WarningSign stroke = makeWarning("Possible stroke warning signs (FAST)", 2, null,
                "dizziness", "headache", "cognitive-fog");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(stroke));

        // Only 1 trigger — should NOT fire
        assertThat(service.evaluateForSymptoms(List.of("dizziness"), null)).isEmpty();

        // 2 triggers — SHOULD fire
        assertThat(service.evaluateForSymptoms(List.of("dizziness", "headache"), null))
                .containsExactly(stroke);
    }

    @Test
    void postpartumWarning_firesOnTwoOfFour() {
        WarningSign postpartum = makeWarning("Postpartum warning signs", 2, null,
                "heavy-periods", "headache", "shortness-of-breath", "chest-discomfort");
        when(warningSignRepository.findByTriggerSymptomSlugIn(anyList())).thenReturn(List.of(postpartum));

        // 1 trigger — should NOT fire
        assertThat(service.evaluateForSymptoms(List.of("headache"), null)).isEmpty();

        // 2 triggers — SHOULD fire
        assertThat(service.evaluateForSymptoms(List.of("heavy-periods", "shortness-of-breath"), null))
                .containsExactly(postpartum);
    }
}
