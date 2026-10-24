package com.heardYou.checkin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.heardYou.condition.ConditionService;
import com.heardYou.symptom.SymptomService;
import com.heardYou.condition.ConditionSummaryDto;
import com.heardYou.symptom.SymptomDetailDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SearchController — 2-character minimum enforcement per spec.
 */
@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock private ConditionService conditionService;
    @Mock private SymptomService symptomService;

    @InjectMocks private SearchController searchController;

    @Test
    void emptyQuery_returnsEmptyResults_noServiceCall() {
        var response = searchController.search("").getBody();
        assertThat(response).isNotNull();
        assertThat((List<?>) response.get("conditions")).isEmpty();
        assertThat((List<?>) response.get("symptoms")).isEmpty();
        verifyNoInteractions(conditionService, symptomService);
    }

    @Test
    void singleCharQuery_returnsEmptyResults_noServiceCall() {
        var response = searchController.search("p").getBody();
        assertThat(response).isNotNull();
        assertThat((List<?>) response.get("conditions")).isEmpty();
        assertThat((List<?>) response.get("symptoms")).isEmpty();
        verifyNoInteractions(conditionService, symptomService);
    }

    @Test
    void twoCharQuery_callsServicesAndReturnsResults() {
        when(conditionService.search("pe")).thenReturn(List.of());
        when(symptomService.search("pe")).thenReturn(List.of());

        var response = searchController.search("pe").getBody();
        assertThat(response).isNotNull();
        assertThat(response.get("query")).isEqualTo("pe");
        verify(conditionService).search("pe");
        verify(symptomService).search("pe");
    }

    @Test
    void whitespaceOnlyQuery_returnsEmptyResults() {
        var response = searchController.search("  ").getBody();
        assertThat(response).isNotNull();
        assertThat((List<?>) response.get("conditions")).isEmpty();
        assertThat((List<?>) response.get("symptoms")).isEmpty();
        verifyNoInteractions(conditionService, symptomService);
    }

    @Test
    void nullQuery_returnsEmptyResults() {
        var response = searchController.search(null).getBody();
        assertThat(response).isNotNull();
        assertThat((List<?>) response.get("conditions")).isEmpty();
        assertThat((List<?>) response.get("symptoms")).isEmpty();
        verifyNoInteractions(conditionService, symptomService);
    }

    @Test
    void queryIsTrimmedBeforeSearching() {
        when(conditionService.search("pelvic")).thenReturn(List.of());
        when(symptomService.search("pelvic")).thenReturn(List.of());

        var response = searchController.search("  pelvic  ").getBody();
        assertThat(response).isNotNull();
        assertThat(response.get("query")).isEqualTo("pelvic");
        verify(conditionService).search("pelvic");
    }

    @Test
    void responseAlwaysContainsQueryField() {
        var response = searchController.search("a").getBody();
        assertThat(response).containsKey("query");
        assertThat(response).containsKey("conditions");
        assertThat(response).containsKey("symptoms");
    }

    // ── regression: real query words that were returning 500 ─────────────────

    @Test
    void searchForPain_returnsConditionsAndSymptoms() {
        // Simulate what the real DB would return for "pain"
        ConditionSummaryDto endoCond = new ConditionSummaryDto(
                1L, "Endometriosis", "endometriosis",
                "A condition causing pain.", "REPRODUCTIVE", "reproductive",
                java.util.Set.of(), java.util.Set.of("reproductive-age"),
                "Gynaecologist", "7-10 years");
        SymptomDetailDto pelvicSymptom = new SymptomDetailDto(
                1L, "Pelvic pain", "pelvic-pain", "Pain in the pelvic region", "pain",
                List.of("cramps", "lower abdominal pain"));

        when(conditionService.search("pain")).thenReturn(List.of(endoCond));
        when(symptomService.search("pain")).thenReturn(List.of(pelvicSymptom));

        var response = searchController.search("pain").getBody();

        assertThat(response).isNotNull();
        assertThat(response.get("query")).isEqualTo("pain");
        assertThat((List<?>) response.get("conditions")).hasSize(1);
        assertThat((List<?>) response.get("symptoms")).hasSize(1);
        verify(conditionService).search("pain");
        verify(symptomService).search("pain");
    }

    @Test
    void searchForEndometriosis_returnsConditionResult() {
        ConditionSummaryDto endoCond = new ConditionSummaryDto(
                1L, "Endometriosis", "endometriosis",
                "A condition causing pain.", "REPRODUCTIVE", "reproductive",
                java.util.Set.of(), java.util.Set.of("reproductive-age"),
                "Gynaecologist", "7-10 years");

        when(conditionService.search("endometriosis")).thenReturn(List.of(endoCond));
        when(symptomService.search("endometriosis")).thenReturn(List.of());

        var response = searchController.search("endometriosis").getBody();

        assertThat(response).isNotNull();
        assertThat(response.get("query")).isEqualTo("endometriosis");
        assertThat((List<?>) response.get("conditions")).hasSize(1);
        assertThat((List<?>) response.get("symptoms")).isEmpty();
        verify(conditionService).search("endometriosis");
        verify(symptomService).search("endometriosis");
    }

    @Test
    void searchWithNoDbMatches_returnsEmptyLists() {
        when(conditionService.search("xyznonexistent")).thenReturn(List.of());
        when(symptomService.search("xyznonexistent")).thenReturn(List.of());

        var response = searchController.search("xyznonexistent").getBody();

        assertThat(response).isNotNull();
        assertThat(response.get("query")).isEqualTo("xyznonexistent");
        assertThat((List<?>) response.get("conditions")).isEmpty();
        assertThat((List<?>) response.get("symptoms")).isEmpty();
    }
}
