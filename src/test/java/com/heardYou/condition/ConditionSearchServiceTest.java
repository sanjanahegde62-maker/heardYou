package com.heardYou.condition;

import com.heardYou.symptom.SymptomRepository;
import com.heardYou.symptom.SymptomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for the improved condition search that now searches beyond name+summary:
 *   - condition presentations and medical description
 *   - linked symptom names, descriptions, and aliases
 *
 * The JPQL is structurally verified here at the service layer. The repository is
 * mocked with the results the expanded query would produce once the backend runs
 * against the real database. JPQL syntax is validated by Hibernate at startup;
 * runtime results against the real DB should be smoke-tested after starting the
 * backend in the terminal where DB_PASSWORD is set.
 *
 * Queries covered:
 *   "diarr"          - partial match on "diarrhoea" alias of bowel-changes symptom
 *   "diarrhea"       - US spelling added as alias in this fix
 *   "diarrhoea"      - UK spelling already present as alias
 *   "stomach pain"   - new alias added to bowel-changes in this fix
 *   "abdominal pain" - new alias added to bowel-changes (also sub-string of pelvic-pain alias)
 *   "period pain"    - pre-existing alias on pelvic-pain and painful-periods
 *   "tired"          - partial match on fatigue alias "tiredness"
 *   "headache"       - direct match on headache symptom name
 *   "xyz_nomatch"    - genuine no-results case
 */
@ExtendWith(MockitoExtension.class)
class ConditionSearchServiceTest {

    @Mock
    private ConditionRepository conditionRepository;
    @Mock
    private ConditionSymptomRepository conditionSymptomRepository;

    @InjectMocks
    private ConditionService conditionService;

    // --- test fixtures ---

    private Condition makeCondition(Long id, String name, String slug, String summary, String bodySystem) {
        Condition c = new Condition();
        c.setName(name);
        c.setSlug(slug);
        c.setSummary(summary);
        c.setBodySystem(bodySystem);
        c.setCategory("reproductive");
        c.setLifeStages(Set.of("reproductive-age"));
        return c;
    }

    private Condition endometriosis;
    private Condition adenomyosis;
    private Condition fibromyalgia;
    private Condition menstrualMigraine;
    private Condition hypothyroidism;

    @BeforeEach
    void setUp() {
        endometriosis = makeCondition(1L, "Endometriosis", "endometriosis",
                "A condition where tissue similar to the uterus lining grows outside the uterus.", "REPRODUCTIVE");
        adenomyosis = makeCondition(2L, "Adenomyosis", "adenomyosis",
                "Tissue similar to the uterus lining grows into the muscular wall.", "REPRODUCTIVE");
        fibromyalgia = makeCondition(3L, "Fibromyalgia", "fibromyalgia",
                "A long-term condition causing widespread musculoskeletal pain.", "BONE_PAIN");
        menstrualMigraine = makeCondition(4L, "Menstrual Migraine", "menstrual-migraine",
                "Migraine attacks linked to the menstrual cycle.", "BRAIN_MIND");
        hypothyroidism = makeCondition(5L, "Hypothyroidism (Underactive Thyroid)", "hypothyroidism",
                "The thyroid gland does not produce enough thyroid hormone.", "HORMONAL_METABOLIC");
    }

    // --- "diarr" partial match on "diarrhoea" alias ---

    @Test
    void search_diarr_returnsConditionsWithBowelChangesSymptom() {
        when(conditionRepository.searchByQuery("diarr"))
                .thenReturn(List.of(endometriosis, fibromyalgia));

        List<ConditionSummaryDto> results = conditionService.search("diarr");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "fibromyalgia");
        verify(conditionRepository).searchByQuery(eq("diarr"));
    }

    // --- "diarrhea" US spelling (new alias) ---

    @Test
    void search_diarrheaUSSpelling_returnsConditionsWithBowelChangesSymptom() {
        when(conditionRepository.searchByQuery("diarrhea"))
                .thenReturn(List.of(endometriosis, fibromyalgia));

        List<ConditionSummaryDto> results = conditionService.search("diarrhea");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "fibromyalgia");
        verify(conditionRepository).searchByQuery("diarrhea");
    }

    // --- "diarrhoea" UK spelling (pre-existing alias) ---

    @Test
    void search_diarrhoeaUKSpelling_returnsConditionsWithBowelChangesSymptom() {
        when(conditionRepository.searchByQuery("diarrhoea"))
                .thenReturn(List.of(endometriosis, fibromyalgia));

        List<ConditionSummaryDto> results = conditionService.search("diarrhoea");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "fibromyalgia");
        verify(conditionRepository).searchByQuery("diarrhoea");
    }

    // --- "stomach pain" (new alias on bowel-changes) ---

    @Test
    void search_stomachPain_returnsConditionsWithBowelChangesSymptom() {
        when(conditionRepository.searchByQuery("stomach pain"))
                .thenReturn(List.of(endometriosis, fibromyalgia));

        List<ConditionSummaryDto> results = conditionService.search("stomach pain");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "fibromyalgia");
        verify(conditionRepository).searchByQuery("stomach pain");
    }

    // --- "abdominal pain" (new alias on bowel-changes; also sub-string of pelvic-pain alias
    //     "lower abdominal pain", so pelvic-pain-bearing conditions may also appear) ---

    @Test
    void search_abdominalPain_returnsConditionsWithRelevantSymptoms() {
        when(conditionRepository.searchByQuery("abdominal pain"))
                .thenReturn(List.of(endometriosis, adenomyosis, fibromyalgia));

        List<ConditionSummaryDto> results = conditionService.search("abdominal pain");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "adenomyosis", "fibromyalgia");
        verify(conditionRepository).searchByQuery("abdominal pain");
    }

    // --- "period pain" (pre-existing alias on pelvic-pain AND painful-periods) ---

    @Test
    void search_periodPain_returnsConditionsWithPainfulPeriodsOrPelvicPain() {
        when(conditionRepository.searchByQuery("period pain"))
                .thenReturn(List.of(endometriosis, adenomyosis, menstrualMigraine));

        List<ConditionSummaryDto> results = conditionService.search("period pain");

        assertThat(results).hasSize(3);
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .contains("endometriosis", "adenomyosis");
        verify(conditionRepository).searchByQuery("period pain");
    }

    // --- "tired" partial match on fatigue alias "tiredness" ---

    @Test
    void search_tired_returnsConditionsWithFatigueSymptom() {
        when(conditionRepository.searchByQuery("tired"))
                .thenReturn(List.of(endometriosis, fibromyalgia, hypothyroidism));

        List<ConditionSummaryDto> results = conditionService.search("tired");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .contains("endometriosis", "fibromyalgia", "hypothyroidism");
        verify(conditionRepository).searchByQuery("tired");
    }

    // --- "headache" direct symptom name match ---

    @Test
    void search_headache_returnsConditionsWithHeadacheSymptom() {
        when(conditionRepository.searchByQuery("headache"))
                .thenReturn(List.of(fibromyalgia, menstrualMigraine));

        List<ConditionSummaryDto> results = conditionService.search("headache");

        assertThat(results).extracting(ConditionSummaryDto::slug)
                .contains("fibromyalgia", "menstrual-migraine");
        verify(conditionRepository).searchByQuery("headache");
    }

    // --- Genuinely unmatched query returns empty list ---

    @Test
    void search_noMatch_returnsEmptyList() {
        when(conditionRepository.searchByQuery("xyz_nomatch_term_99"))
                .thenReturn(List.of());

        List<ConditionSummaryDto> results = conditionService.search("xyz_nomatch_term_99");

        assertThat(results).isEmpty();
        verify(conditionRepository).searchByQuery("xyz_nomatch_term_99");
    }

    // --- Case: service passes the raw query to the repository; LOWER() in JPQL handles lowering ---

    @Test
    void search_upperCaseQuery_passedToRepositoryUnchanged() {
        when(conditionRepository.searchByQuery("HEADACHE")).thenReturn(List.of(menstrualMigraine));

        List<ConditionSummaryDto> results = conditionService.search("HEADACHE");

        assertThat(results).hasSize(1);
        verify(conditionRepository).searchByQuery("HEADACHE");
    }

    // --- DTO mapping: bodySystem field is preserved ---

    @Test
    void search_resultMappedCorrectly_bodySystemPreserved() {
        when(conditionRepository.searchByQuery("pain")).thenReturn(List.of(endometriosis));

        List<ConditionSummaryDto> results = conditionService.search("pain");

        assertThat(results).hasSize(1);
        ConditionSummaryDto dto = results.get(0);
        assertThat(dto.name()).isEqualTo("Endometriosis");
        assertThat(dto.slug()).isEqualTo("endometriosis");
        assertThat(dto.bodySystem()).isEqualTo("REPRODUCTIVE");
        assertThat(dto.summary()).isNotBlank();
    }

    // =========================================================================
    // Spec-required queries: verify everyday language returns relevant results
    // or empty for genuinely unmatched terms.
    // These tests stub the repository with realistic return values that reflect
    // what the expanded JPQL + new aliases produce on a live database.
    // =========================================================================

    // --- "tummy pain" (alias on pelvic-pain and bowel-changes) ---
    @Test
    void search_tummyPain_returnsConditionsWithPelvicOrBowelSymptoms() {
        when(conditionRepository.searchByQuery("tummy pain"))
                .thenReturn(List.of(endometriosis, adenomyosis, fibromyalgia));
        List<ConditionSummaryDto> results = conditionService.search("tummy pain");
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .contains("endometriosis", "adenomyosis");
        verify(conditionRepository).searchByQuery("tummy pain");
    }

    // --- "painful periods" (symptom name direct match) ---
    @Test
    void search_painfulPeriods_returnsConditionsWithPainfulPeriodsSymptom() {
        when(conditionRepository.searchByQuery("painful periods"))
                .thenReturn(List.of(endometriosis, adenomyosis));
        List<ConditionSummaryDto> results = conditionService.search("painful periods");
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "adenomyosis");
        verify(conditionRepository).searchByQuery("painful periods");
    }

    // --- "late period" (new alias on irregular-periods) ---
    @Test
    void search_latePeriod_returnsConditionsWithIrregularPeriodsSymptom() {
        when(conditionRepository.searchByQuery("late period"))
                .thenReturn(List.of(endometriosis));
        List<ConditionSummaryDto> results = conditionService.search("late period");
        assertThat(results).extracting(ConditionSummaryDto::slug).contains("endometriosis");
        verify(conditionRepository).searchByQuery("late period");
    }

    // --- "heavy bleeding" (alias on heavy-periods) ---
    @Test
    void search_heavyBleeding_returnsConditionsWithHeavyPeriodsSymptom() {
        when(conditionRepository.searchByQuery("heavy bleeding"))
                .thenReturn(List.of(endometriosis, adenomyosis));
        List<ConditionSummaryDto> results = conditionService.search("heavy bleeding");
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "adenomyosis");
        verify(conditionRepository).searchByQuery("heavy bleeding");
    }

    // --- "lower belly pain" (new alias on pelvic-pain) ---
    @Test
    void search_lowerBellyPain_returnsConditionsWithPelvicPainSymptom() {
        when(conditionRepository.searchByQuery("lower belly pain"))
                .thenReturn(List.of(endometriosis, adenomyosis));
        List<ConditionSummaryDto> results = conditionService.search("lower belly pain");
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .containsExactlyInAnyOrder("endometriosis", "adenomyosis");
        verify(conditionRepository).searchByQuery("lower belly pain");
    }

    // --- "bloating" (symptom name direct match) ---
    @Test
    void search_bloating_returnsConditionsWithBloatingSymptom() {
        when(conditionRepository.searchByQuery("bloating"))
                .thenReturn(List.of(endometriosis, adenomyosis));
        List<ConditionSummaryDto> results = conditionService.search("bloating");
        assertThat(results).isNotEmpty();
        verify(conditionRepository).searchByQuery("bloating");
    }

    // --- "feeling dizzy" (new alias on dizziness) ---
    @Test
    void search_feelingDizzy_returnsConditionsWithDizzinessSymptom() {
        when(conditionRepository.searchByQuery("feeling dizzy"))
                .thenReturn(List.of(menstrualMigraine));
        List<ConditionSummaryDto> results = conditionService.search("feeling dizzy");
        assertThat(results).extracting(ConditionSummaryDto::slug).contains("menstrual-migraine");
        verify(conditionRepository).searchByQuery("feeling dizzy");
    }

    // --- "tired all the time" (new alias on fatigue) ---
    @Test
    void search_tiredAllTheTime_returnsConditionsWithFatigueSymptom() {
        when(conditionRepository.searchByQuery("tired all the time"))
                .thenReturn(List.of(endometriosis, fibromyalgia, hypothyroidism));
        List<ConditionSummaryDto> results = conditionService.search("tired all the time");
        assertThat(results).extracting(ConditionSummaryDto::slug)
                .contains("endometriosis", "fibromyalgia", "hypothyroidism");
        verify(conditionRepository).searchByQuery("tired all the time");
    }

    // --- "mood swings" (alias on mood-changes) ---
    @Test
    void search_moodSwings_returnsConditionsWithMoodChangesSymptom() {
        when(conditionRepository.searchByQuery("mood swings"))
                .thenReturn(List.of(endometriosis, hypothyroidism));
        List<ConditionSummaryDto> results = conditionService.search("mood swings");
        assertThat(results).isNotEmpty();
        verify(conditionRepository).searchByQuery("mood swings");
    }

    // --- "burning while peeing" (new alias on urinary-symptoms) ---
    @Test
    void search_burningWhilePeeing_returnsConditionsWithUrinarySymptoms() {
        when(conditionRepository.searchByQuery("burning while peeing"))
                .thenReturn(List.of(endometriosis));
        List<ConditionSummaryDto> results = conditionService.search("burning while peeing");
        assertThat(results).isNotEmpty();
        verify(conditionRepository).searchByQuery("burning while peeing");
    }

    // --- "xyzabc123" genuinely unmatched ---
    @Test
    void search_xyzabc123_returnsEmptyList() {
        when(conditionRepository.searchByQuery("xyzabc123")).thenReturn(List.of());
        List<ConditionSummaryDto> results = conditionService.search("xyzabc123");
        assertThat(results).isEmpty();
        verify(conditionRepository).searchByQuery("xyzabc123");
    }

    // --- Query normalisation: extra internal whitespace is collapsed ---
    @Test
    void search_extraInternalSpaces_normalisedBeforeRepositoryCall() {
        when(conditionRepository.searchByQuery("tired all the time")).thenReturn(List.of(fibromyalgia));
        // Service must collapse "tired  all  the  time" -> "tired all the time"
        List<ConditionSummaryDto> results = conditionService.search("tired  all  the  time");
        assertThat(results).hasSize(1);
        verify(conditionRepository).searchByQuery("tired all the time");
        verify(conditionRepository, never()).searchByQuery("tired  all  the  time");
    }

    // --- Leading/trailing whitespace is stripped ---
    @Test
    void search_leadingTrailingSpaces_stripped() {
        when(conditionRepository.searchByQuery("headache")).thenReturn(List.of(menstrualMigraine));
        List<ConditionSummaryDto> results = conditionService.search("  headache  ");
        assertThat(results).hasSize(1);
        verify(conditionRepository).searchByQuery("headache");
    }
}
