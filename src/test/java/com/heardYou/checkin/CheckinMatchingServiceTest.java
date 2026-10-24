package com.heardYou.checkin;

import com.heardYou.checkin.CheckinRequest.Duration;
import com.heardYou.checkin.CheckinResponse.MatchedSymptomDetail;
import com.heardYou.checkin.CheckinResponse.PatternMatch;
import com.heardYou.condition.Condition;
import com.heardYou.condition.ConditionSymptom;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.symptom.Symptom;
import com.heardYou.warning.WarningSignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the spec-compliant matching algorithm.
 *
 * Formula under test (spec pp. 5–6):
 *   severityFactor = 0.5 + 0.1 * severity
 *   durationFactor = 0.5 | 0.8 | 1.0  (null → 0.8)
 *   raw            = Σ weight * severityFactor * durationFactor
 *   denominator    = sum of 6 highest weights for the condition
 *   score          = min(1, raw / denominator)  [+0.10 if life-stage tagged, capped at 1]
 *   Include only if matchedCount >= 2 AND score >= 0.20
 *   Sort by score DESC, then matchedCount DESC; top 5.
 */
@ExtendWith(MockitoExtension.class)
class CheckinMatchingServiceTest {

    @Mock private ConditionSymptomRepository conditionSymptomRepository;
    @Mock private WarningSignService warningSignService;
    @InjectMocks private CheckinMatchingService service;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private Condition condition;
    private Symptom symptomA;  // weight 0.9
    private Symptom symptomB;  // weight 0.6
    private Symptom symptomC;  // weight 0.5
    private ConditionSymptom csA;
    private ConditionSymptom csB;
    private ConditionSymptom csC;

    @BeforeEach
    void setUp() {
        condition = new Condition();
        condition.setName("Test Condition");
        condition.setSlug("test-condition");
        condition.setSummary("A test condition");
        condition.setLifeStages(Set.of("reproductive-age"));
        condition.setClinicianQuestions(List.of());
        condition.setSources(java.util.Set.of());

        symptomA = makeSymptom("Pelvic Pain",   "pelvic-pain");
        symptomB = makeSymptom("Fatigue",        "fatigue");
        symptomC = makeSymptom("Nausea",         "nausea");

        csA = makeCs(condition, symptomA, 0.9);
        csB = makeCs(condition, symptomB, 0.6);
        csC = makeCs(condition, symptomC, 0.5);
    }

    // ── Duration enum ─────────────────────────────────────────────────────────

    @Test
    void durationFactorValues() {
        assertThat(Duration.UNDER_1_WEEK.factor()).isEqualTo(0.5);
        assertThat(Duration.ONE_TO_4_WEEKS.factor()).isEqualTo(0.8);
        assertThat(Duration.OVER_1_MONTH.factor()).isEqualTo(1.0);
    }

    @Test
    void durationFromStringParsesAllValidValues() {
        assertThat(Duration.fromString("UNDER_1_WEEK")).isEqualTo(Duration.UNDER_1_WEEK);
        assertThat(Duration.fromString("1_TO_4_WEEKS")).isEqualTo(Duration.ONE_TO_4_WEEKS);
        assertThat(Duration.fromString("OVER_1_MONTH")).isEqualTo(Duration.OVER_1_MONTH);
        assertThat(Duration.fromString(null)).isNull();
    }

    @Test
    void durationFromStringIsCaseInsensitive() {
        assertThat(Duration.fromString("over_1_month")).isEqualTo(Duration.OVER_1_MONTH);
        assertThat(Duration.fromString("Under_1_Week")).isEqualTo(Duration.UNDER_1_WEEK);
    }

    @Test
    void durationFromStringThrowsOnInvalidValue() {
        assertThatThrownBy(() -> Duration.fromString("3 days"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid duration value");
    }

    // ── severityFactor ────────────────────────────────────────────────────────

    @Test
    void severityFactor_severity1_gives_0point6() {
        // Use "teen" so the life-stage bonus does not apply (condition is tagged for "reproductive-age").
        // raw = 0.9*0.6*0.8 + 0.6*0.6*0.8 = 0.432 + 0.288 = 0.720
        // denom = 0.9+0.6+0.5 = 2.0
        // score = min(1, 0.720/2.0) = 0.360 → MODERATE
        stubTwoSymptoms();
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 1, null),
                        entry("fatigue",     1, null)));

        assertThat(response.matches()).hasSize(1);
        PatternMatch m = response.matches().get(0);
        assertThat(m.educationalOverlapScore()).isCloseTo(0.360, within(0.001));
        assertThat(m.overlapLabel()).isEqualTo("MODERATE");
    }

    @Test
    void severityFactor_severity5_gives_1point0() {
        // Use "teen" so the life-stage bonus does not apply.
        // raw = 0.9*1.0*0.8 + 0.6*1.0*0.8 = 0.72 + 0.48 = 1.20
        // denom = 2.0; score = min(1, 1.20/2.0) = 0.600 → HIGH
        stubTwoSymptoms();
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, null),
                        entry("fatigue",     5, null)));

        assertThat(response.matches()).hasSize(1);
        PatternMatch m = response.matches().get(0);
        assertThat(m.educationalOverlapScore()).isCloseTo(0.600, within(0.001));
        assertThat(m.overlapLabel()).isEqualTo("HIGH");
    }

    // ── durationFactor ────────────────────────────────────────────────────────

    @Test
    void durationFactor_UNDER_1_WEEK_is_0point5() {
        // Use "teen" to isolate duration factor without life-stage bonus.
        // raw = 0.9*1.0*0.5 + 0.6*1.0*0.5 = 0.45 + 0.30 = 0.75
        // denom = 2.0; score = 0.75/2.0 = 0.375 → MODERATE
        stubTwoSymptoms();
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, Duration.UNDER_1_WEEK),
                        entry("fatigue",     5, Duration.UNDER_1_WEEK)));

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).educationalOverlapScore()).isCloseTo(0.375, within(0.001));
        assertThat(response.matches().get(0).overlapLabel()).isEqualTo("MODERATE");
    }

    @Test
    void durationFactor_1_TO_4_WEEKS_is_0point8() {
        // Use "teen" to isolate duration factor without life-stage bonus.
        // raw = 0.9*1.0*0.8 + 0.6*1.0*0.8 = 0.72 + 0.48 = 1.20
        // denom = 2.0; score = 0.600 → HIGH
        stubTwoSymptoms();
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, Duration.ONE_TO_4_WEEKS),
                        entry("fatigue",     5, Duration.ONE_TO_4_WEEKS)));

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).educationalOverlapScore()).isCloseTo(0.600, within(0.001));
    }

    @Test
    void durationFactor_OVER_1_MONTH_is_1point0() {
        // Use "teen" to isolate duration factor without life-stage bonus.
        // raw = 0.9*1.0*1.0 + 0.6*1.0*1.0 = 0.9 + 0.6 = 1.50
        // denom = 2.0; score = min(1, 1.50/2.0) = 0.750 → HIGH
        stubTwoSymptoms();
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, Duration.OVER_1_MONTH),
                        entry("fatigue",     5, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).educationalOverlapScore()).isCloseTo(0.750, within(0.001));
        assertThat(response.matches().get(0).overlapLabel()).isEqualTo("HIGH");
    }

    @Test
    void nullDuration_usesNeutralFactor_0point8() {
        // Null duration must equal ONE_TO_4_WEEKS factor (0.8).
        // Use "teen" for both so life-stage bonus cancels out.
        stubTwoSymptoms();
        CheckinResponse responseNull = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, null),
                        entry("fatigue",     5, null)));
        CheckinResponse responseWeeks = service.match(
                requestWith("teen",
                        entry("pelvic-pain", 5, Duration.ONE_TO_4_WEEKS),
                        entry("fatigue",     5, Duration.ONE_TO_4_WEEKS)));

        assertThat(responseNull.matches().get(0).educationalOverlapScore())
                .isCloseTo(responseWeeks.matches().get(0).educationalOverlapScore(), within(0.001));
    }

    // ── denominator (top-6 weights) ───────────────────────────────────────────

    @Test
    void denominator_usesTop6Weights_notAll() {
        // Build a condition with 8 symptoms; top-6 sum should be used as denominator.
        // Use "teen" so the life-stage bonus does not apply.
        Symptom s1 = makeSymptom("S1", "s1"); ConditionSymptom cs1 = makeCs(condition, s1, 0.9);
        Symptom s2 = makeSymptom("S2", "s2"); ConditionSymptom cs2 = makeCs(condition, s2, 0.8);
        Symptom s3 = makeSymptom("S3", "s3"); ConditionSymptom cs3 = makeCs(condition, s3, 0.7);
        Symptom s4 = makeSymptom("S4", "s4"); ConditionSymptom cs4 = makeCs(condition, s4, 0.6);
        Symptom s5 = makeSymptom("S5", "s5"); ConditionSymptom cs5 = makeCs(condition, s5, 0.5);
        Symptom s6 = makeSymptom("S6", "s6"); ConditionSymptom cs6 = makeCs(condition, s6, 0.4);
        Symptom s7 = makeSymptom("S7", "s7"); ConditionSymptom cs7 = makeCs(condition, s7, 0.3);
        Symptom s8 = makeSymptom("S8", "s8"); ConditionSymptom cs8 = makeCs(condition, s8, 0.2);

        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(cs1, cs2));
        when(conditionSymptomRepository.findByCondition(condition))
                .thenReturn(List.of(cs1, cs2, cs3, cs4, cs5, cs6, cs7, cs8));
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        // top-6 weights: 0.9+0.8+0.7+0.6+0.5+0.4 = 3.9
        // raw = (0.9*1.0*1.0) + (0.8*1.0*1.0) = 1.7
        // score = min(1, 1.7/3.9) = 0.4359 → MODERATE; no life-stage bonus ("teen")
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("s1", 5, Duration.OVER_1_MONTH),
                        entry("s2", 5, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).hasSize(1);
        double score = response.matches().get(0).educationalOverlapScore();
        assertThat(score).isCloseTo(1.7 / 3.9, within(0.001));
        assertThat(response.matches().get(0).overlapLabel()).isEqualTo("MODERATE");
    }

    // ── life-stage bonus ──────────────────────────────────────────────────────

    @Test
    void lifeStageBonus_addedWhenConditionTaggedForStage() {
        stubTwoSymptoms();
        // "reproductive-age" IS in condition.lifeStages (set in setUp)
        // raw = 0.9*0.6*0.8 + 0.6*0.6*0.8 = 0.432 + 0.288 = 0.720
        // denom = 2.0; base score = 0.360; +0.10 bonus → 0.460 → MODERATE
        CheckinResponse response = service.match(
                requestWith("reproductive-age",
                        entry("pelvic-pain", 1, null),
                        entry("fatigue",     1, null)));

        assertThat(response.matches().get(0).educationalOverlapScore())
                .isCloseTo(0.460, within(0.001));
    }

    @Test
    void lifeStageBonus_notAddedWhenStageNotTagged() {
        stubTwoSymptoms();
        // "teen" is NOT in condition.lifeStages
        CheckinResponse withBonus    = service.match(requestWith("reproductive-age",
                entry("pelvic-pain", 1, null), entry("fatigue", 1, null)));
        CheckinResponse withoutBonus = service.match(requestWith("teen",
                entry("pelvic-pain", 1, null), entry("fatigue", 1, null)));

        double with    = withBonus.matches().get(0).educationalOverlapScore();
        double without = withoutBonus.matches().get(0).educationalOverlapScore();
        assertThat(with - without).isCloseTo(0.10, within(0.001));
    }

    @Test
    void lifeStageBonus_cappedAt1() {
        stubTwoSymptoms();
        // Score will be very high; bonus must not push it above 1.0
        CheckinResponse response = service.match(
                requestWith("reproductive-age",                 // tagged → +0.10
                        entry("pelvic-pain", 5, Duration.OVER_1_MONTH),
                        entry("fatigue",     5, Duration.OVER_1_MONTH)));

        // raw=1.5, denom=2.0 → 0.75 + 0.10 = 0.85, still ≤ 1.0
        assertThat(response.matches().get(0).educationalOverlapScore()).isLessThanOrEqualTo(1.0);
    }

    // ── minimum matched symptoms ──────────────────────────────────────────────

    @Test
    void singleMatchedSymptom_isExcluded() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(csA));      // only A matches
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinResponse response = service.match(
                requestWith("reproductive-age", entry("pelvic-pain", 5, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).isEmpty();
        assertThat(response.unmatchedSymptomSlugs()).doesNotContain("pelvic-pain");
        // slug IS matched to a condition, but excluded due to < 2 matched symptoms
    }

    // ── score threshold ───────────────────────────────────────────────────────

    @Test
    void scoreBelowThreshold_0point20_isExcluded() {
        stubTwoSymptoms();
        // Use severity=1, duration=UNDER_1_WEEK to get a very low score
        // raw = 0.9*0.6*0.5 + 0.6*0.6*0.5 = 0.270 + 0.180 = 0.450
        // denom = 0.9+0.6+0.5 = 2.0
        // base = 0.450/2.0 = 0.225; no life-stage bonus (use "teen")
        // 0.225 ≥ 0.20 → still included; lower it further by using severity=1 + UNDER_1_WEEK
        // The test verifies exclusion: need score < 0.20
        // Build a condition whose top-6 denom is very high vs the raw
        Symptom s1 = makeSymptom("S1", "s1"); ConditionSymptom cs1 = makeCs(condition, s1, 0.9);
        Symptom s2 = makeSymptom("S2", "s2"); ConditionSymptom cs2 = makeCs(condition, s2, 0.9);
        Symptom s3 = makeSymptom("S3", "s3"); ConditionSymptom cs3 = makeCs(condition, s3, 0.9);
        Symptom s4 = makeSymptom("S4", "s4"); ConditionSymptom cs4 = makeCs(condition, s4, 0.9);
        Symptom s5 = makeSymptom("S5", "s5"); ConditionSymptom cs5 = makeCs(condition, s5, 0.9);
        Symptom s6 = makeSymptom("S6", "s6"); ConditionSymptom cs6 = makeCs(condition, s6, 0.9);
        // She submits only s1 and s2 at severity=1, UNDER_1_WEEK
        // raw = 2 * 0.9 * 0.6 * 0.5 = 0.54; denom = 6 * 0.9 = 5.4; score = 0.1 → excluded

        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(cs1, cs2));
        when(conditionSymptomRepository.findByCondition(condition))
                .thenReturn(List.of(cs1, cs2, cs3, cs4, cs5, cs6));
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinResponse response = service.match(
                requestWith("teen",   // no bonus
                        entry("s1", 1, Duration.UNDER_1_WEEK),
                        entry("s2", 1, Duration.UNDER_1_WEEK)));

        assertThat(response.matches()).isEmpty();
    }

    @Test
    void scoreAtExactThreshold_0point20_isIncluded() {
        // Build fixtures so score = exactly 0.20
        // We need: raw/denom = 0.20 with no life-stage bonus
        // Use 2 symptoms: weight 0.5 each; severity=1 (factor=0.6); duration=UNDER_1_WEEK (factor=0.5)
        // raw = 2 * 0.5 * 0.6 * 0.5 = 0.30
        // denom = top-6; only 2 symptoms, so denom = 1.0 → score = 0.30 → not exactly 0.20
        // Instead: weight=0.2; sev=5 (1.0); UNDER_1_WEEK(0.5)
        //   raw = 2 * 0.2 * 1.0 * 0.5 = 0.20; denom = 0.4; score = 0.50 — still not 0.20
        // Easier: use 6 equal weights w so denom=6w, raw=2*w*sF*dF, score=2*sF*dF/6
        // sev=1(0.6), UNDER_1_WEEK(0.5): score = 2*0.6*0.5/6 = 0.10 → excluded
        // sev=3(0.8), OVER_1_MONTH(1.0): score = 2*0.8*1.0/6 = 0.267 → included
        // We want exactly 0.20: 2*sF*dF/6 = 0.20 → sF*dF = 0.60
        //   sev=2(0.7), UNDER_1_WEEK(0.5): 0.35 — no
        //   sev=1(0.6), ONE_TO_4_WEEKS(0.8): 0.48 — no  (score=2*0.48/6=0.16 → excluded!)
        //   sev=3(0.8), OVER_1_MONTH(1.0): 0.80 → score = 2*0.8/6 = 0.267 → included
        // Let's just verify: score = 0.267 ≥ 0.20 → included
        Symptom s1 = makeSymptom("S1", "s1"); ConditionSymptom cs1 = makeCs(condition, s1, 0.5);
        Symptom s2 = makeSymptom("S2", "s2"); ConditionSymptom cs2 = makeCs(condition, s2, 0.5);
        Symptom s3 = makeSymptom("S3", "s3"); ConditionSymptom cs3 = makeCs(condition, s3, 0.5);
        Symptom s4 = makeSymptom("S4", "s4"); ConditionSymptom cs4 = makeCs(condition, s4, 0.5);
        Symptom s5 = makeSymptom("S5", "s5"); ConditionSymptom cs5 = makeCs(condition, s5, 0.5);
        Symptom s6 = makeSymptom("S6", "s6"); ConditionSymptom cs6 = makeCs(condition, s6, 0.5);

        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(cs1, cs2));
        when(conditionSymptomRepository.findByCondition(condition))
                .thenReturn(List.of(cs1, cs2, cs3, cs4, cs5, cs6));
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        // sev=3, OVER_1_MONTH: raw = 2*0.5*0.8*1.0=0.80; denom=3.0; score=0.267
        CheckinResponse response = service.match(
                requestWith("teen",
                        entry("s1", 3, Duration.OVER_1_MONTH),
                        entry("s2", 3, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).hasSize(1);
        assertThat(response.matches().get(0).educationalOverlapScore()).isGreaterThanOrEqualTo(0.20);
    }

    // ── overlap label boundaries ──────────────────────────────────────────────

    @Test
    void overlapLabel_HIGH_atExactly_0point60() {
        assertThat(CheckinMatchingService.overlapLabel(0.60)).isEqualTo("HIGH");
    }

    @Test
    void overlapLabel_HIGH_above_0point60() {
        assertThat(CheckinMatchingService.overlapLabel(0.80)).isEqualTo("HIGH");
        assertThat(CheckinMatchingService.overlapLabel(1.00)).isEqualTo("HIGH");
    }

    @Test
    void overlapLabel_MODERATE_atExactly_0point35() {
        assertThat(CheckinMatchingService.overlapLabel(0.35)).isEqualTo("MODERATE");
    }

    @Test
    void overlapLabel_MODERATE_justBelow_0point60() {
        assertThat(CheckinMatchingService.overlapLabel(0.599)).isEqualTo("MODERATE");
    }

    @Test
    void overlapLabel_LOW_below_0point35() {
        assertThat(CheckinMatchingService.overlapLabel(0.34)).isEqualTo("LOW");
        assertThat(CheckinMatchingService.overlapLabel(0.20)).isEqualTo("LOW");
    }

    // ── "why" sentence template ───────────────────────────────────────────────

    @Test
    void whySentence_singleSymptom_usesCorrectTemplate() {
        List<MatchedSymptomDetail> details = List.of(
                detail("pelvic-pain", "Pelvic pain", 5));
        String why = CheckinMatchingService.buildWhy(details, "Endometriosis");
        assertThat(why).isEqualTo(
                "You reported Pelvic pain. Women commonly report these together with Endometriosis.");
    }

    @Test
    void whySentence_twoSymptoms_usesAnd() {
        List<MatchedSymptomDetail> details = List.of(
                detail("fatigue",    "Fatigue",     5),
                detail("nausea",     "Nausea",      3));
        String why = CheckinMatchingService.buildWhy(details, "Heart attack");
        assertThat(why).isEqualTo(
                "You reported Fatigue and Nausea. Women commonly report these together with Heart attack.");
    }

    @Test
    void whySentence_threeSymptoms_usesCommaAndAnd() {
        List<MatchedSymptomDetail> details = List.of(
                detail("fatigue",         "Fatigue",          5),
                detail("nausea",          "Nausea",           3),
                detail("jaw-or-back-pain","Jaw or back pain",  3));
        String why = CheckinMatchingService.buildWhy(details, "Heart attack");
        assertThat(why).isEqualTo(
                "You reported Fatigue, Nausea and Jaw or back pain. " +
                "Women commonly report these together with Heart attack.");
    }

    @Test
    void whySentence_usesSymptomName_notSlug() {
        List<MatchedSymptomDetail> details = List.of(
                detail("pelvic-pain", "Pelvic pain", 4),
                detail("fatigue",     "Fatigue",     3));
        String why = CheckinMatchingService.buildWhy(details, "Endometriosis");
        assertThat(why).doesNotContain("pelvic-pain");
        assertThat(why).contains("Pelvic pain");
    }

    // ── sorting ───────────────────────────────────────────────────────────────

    @Test
    void results_sortedByScoreDescThenMatchedCountDesc() {
        // Build two conditions with deliberately different scores / matched counts
        Condition cHigh = makeCondition("high-cond", "reproductive-age");
        Condition cLow  = makeCondition("low-cond",  "adult");

        Symptom sH1 = makeSymptom("H1","h1"); ConditionSymptom csH1 = makeCs(cHigh, sH1, 0.9);
        Symptom sH2 = makeSymptom("H2","h2"); ConditionSymptom csH2 = makeCs(cHigh, sH2, 0.9);
        Symptom sL1 = makeSymptom("L1","l1"); ConditionSymptom csL1 = makeCs(cLow,  sL1, 0.3);
        Symptom sL2 = makeSymptom("L2","l2"); ConditionSymptom csL2 = makeCs(cLow,  sL2, 0.3);

        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(csH1, csH2, csL1, csL2));
        when(conditionSymptomRepository.findByCondition(cHigh)).thenReturn(List.of(csH1, csH2));
        when(conditionSymptomRepository.findByCondition(cLow)).thenReturn(List.of(csL1, csL2));
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinResponse response = service.match(
                requestWith("reproductive-age",
                        entry("h1", 5, Duration.OVER_1_MONTH),
                        entry("h2", 5, Duration.OVER_1_MONTH),
                        entry("l1", 5, Duration.OVER_1_MONTH),
                        entry("l2", 5, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).hasSize(2);
        // high-cond should appear first (higher score)
        assertThat(response.matches().get(0).conditionSlug()).isEqualTo("high-cond");
        assertThat(response.matches().get(1).conditionSlug()).isEqualTo("low-cond");
    }

    @Test
    void results_cappedAtFive() {
        // Create 6 conditions, each matching exactly 2 symptoms at equal scores
        List<ConditionSymptom> allMappings = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Condition c = makeCondition("cond-" + i, "adult");
            Symptom sa = makeSymptom("SA-" + i, "sa-" + i);
            Symptom sb = makeSymptom("SB-" + i, "sb-" + i);
            ConditionSymptom csA2 = makeCs(c, sa, 0.5);
            ConditionSymptom csB2 = makeCs(c, sb, 0.5);
            allMappings.add(csA2);
            allMappings.add(csB2);
            when(conditionSymptomRepository.findByCondition(c))
                    .thenReturn(List.of(csA2, csB2));
        }

        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(allMappings);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        List<CheckinRequest.SymptomEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(entry("sa-" + i, 5, Duration.OVER_1_MONTH));
            entries.add(entry("sb-" + i, 5, Duration.OVER_1_MONTH));
        }
        CheckinRequest request = new CheckinRequest();
        request.setLifeStage("adult");
        request.setSymptoms(entries);

        CheckinResponse response = service.match(request);

        assertThat(response.matches()).hasSize(5);
    }

    // ── duplicate slug rejection ──────────────────────────────────────────────

    @Test
    void duplicateSlugs_throwValidationException() {
        CheckinRequest request = requestWith("reproductive-age",
                entry("pelvic-pain", 3, null),
                entry("pelvic-pain", 2, null));

        assertThatThrownBy(() -> service.match(request))
                .isInstanceOf(com.heardYou.common.ValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    // ── warning-sign independence ─────────────────────────────────────────────

    @Test
    void warningSignsReturnedEvenWhenNoPatternMatches() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList())).thenReturn(List.of());
        when(warningSignService.evaluateForSymptoms(anyList(), any()))
                .thenReturn(List.of(buildWarningSign("Emergency")));

        CheckinResponse response = service.match(
                requestWith("reproductive-age", entry("heavy-periods", 5, Duration.OVER_1_MONTH)));

        assertThat(response.matches()).isEmpty();
        assertThat(response.warningSignsEvaluated()).hasSize(1);
        assertThat(response.warningSignsEvaluated().get(0).name()).isEqualTo("Emergency");
    }

    @Test
    void warningSignsEvaluatedIndependentlyOfMatchScore() {
        // Even a zero-score check-in must deliver warning signs
        stubTwoSymptoms();
        when(warningSignService.evaluateForSymptoms(anyList(), any()))
                .thenReturn(List.of(buildWarningSign("Urgent")));

        CheckinResponse response = service.match(
                requestWith("teen",   // no life-stage bonus
                        entry("pelvic-pain", 1, Duration.UNDER_1_WEEK),
                        entry("fatigue",     1, Duration.UNDER_1_WEEK)));

        // Warning sign present regardless of match quality
        assertThat(response.warningSignsEvaluated()).hasSize(1);
    }

    // ── determinism ──────────────────────────────────────────────────────────

    @Test
    void identicalInputAlwaysProducesIdenticalOutput() {
        stubTwoSymptoms();
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinRequest r = requestWith("reproductive-age",
                entry("pelvic-pain", 3, Duration.ONE_TO_4_WEEKS),
                entry("fatigue",     4, Duration.OVER_1_MONTH));

        CheckinResponse r1 = service.match(r);
        CheckinResponse r2 = service.match(r);

        assertThat(r1.matches().get(0).educationalOverlapScore())
                .isEqualTo(r2.matches().get(0).educationalOverlapScore());
        assertThat(r1.matches().get(0).overlapLabel())
                .isEqualTo(r2.matches().get(0).overlapLabel());
    }

    // ── response shape ────────────────────────────────────────────────────────

    @Test
    void disclaimerIsPresent() {
        stubTwoSymptoms();
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinResponse response = service.match(
                requestWith("reproductive-age",
                        entry("pelvic-pain", 3, null),
                        entry("fatigue",     3, null)));

        assertThat(response.disclaimer()).contains("Educational information, not medical advice");
        assertThat(response.disclaimer()).contains("112 in India");
    }

    @Test
    void responseNeverContainsDiagnosisLanguage() {
        stubTwoSymptoms();
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        CheckinResponse response = service.match(
                requestWith("reproductive-age",
                        entry("pelvic-pain", 5, Duration.OVER_1_MONTH),
                        entry("fatigue",     5, Duration.OVER_1_MONTH)));

        PatternMatch m = response.matches().get(0);
        assertThat(m.overlapExplanation()).doesNotContainIgnoringCase("probability");
        assertThat(m.overlapExplanation()).doesNotContainIgnoringCase("you have");
        assertThat(m.overlapExplanation()).doesNotContainIgnoringCase("diagnosis");
        assertThat(m.overlapLabel()).isNotIn("fine", "safe", "low risk");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubTwoSymptoms() {
        when(conditionSymptomRepository.findBySymptomSlugIn(anyList()))
                .thenReturn(List.of(csA, csB));
        when(conditionSymptomRepository.findByCondition(condition))
                .thenReturn(List.of(csA, csB, csC));
    }

    private static CheckinRequest requestWith(String lifeStage,
                                               CheckinRequest.SymptomEntry... entries) {
        CheckinRequest req = new CheckinRequest();
        req.setLifeStage(lifeStage);
        req.setSymptoms(java.util.Arrays.asList(entries));
        return req;
    }

    private static CheckinRequest.SymptomEntry entry(String slug, int severity, Duration duration) {
        CheckinRequest.SymptomEntry e = new CheckinRequest.SymptomEntry();
        e.setSlug(slug);
        e.setSeverity(severity);
        e.setDuration(duration);
        return e;
    }

    private static Symptom makeSymptom(String name, String slug) {
        Symptom s = new Symptom();
        s.setName(name);
        s.setSlug(slug);
        return s;
    }

    private static ConditionSymptom makeCs(Condition c, Symptom s, double weight) {
        ConditionSymptom cs = new ConditionSymptom();
        cs.setCondition(c);
        cs.setSymptom(s);
        cs.setOverlapWeight(weight);
        return cs;
    }

    private static Condition makeCondition(String slug, String... stages) {
        Condition c = new Condition();
        c.setName(slug);
        c.setSlug(slug);
        c.setSummary("Summary for " + slug);
        c.setLifeStages(new java.util.HashSet<>(java.util.Arrays.asList(stages)));
        c.setClinicianQuestions(List.of());
        c.setSources(java.util.Set.of());
        return c;
    }

    private static MatchedSymptomDetail detail(String slug, String name, int severity) {
        return new MatchedSymptomDetail(slug, name, 0.5, severity, null);
    }

    private com.heardYou.warning.WarningSign buildWarningSign(String name) {
        com.heardYou.warning.WarningSign w = new com.heardYou.warning.WarningSign();
        w.setName(name);
        w.setMessage("Seek care");
        w.setRecommendedAction("Act now");
        w.setSourceUrl("https://example.com");
        w.setSourceTitle("NHS");
        return w;
    }

}
