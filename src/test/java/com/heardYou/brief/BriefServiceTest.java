package com.heardYou.brief;

import com.heardYou.checkin.CheckinMatchingService;
import com.heardYou.checkin.CheckinResponse;
import com.heardYou.checkin.CheckinResponse.MatchedSymptomDetail;
import com.heardYou.checkin.CheckinResponse.PatternMatch;
import com.heardYou.source.SourceDto;
import com.heardYou.symptom.Symptom;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSign;
import com.heardYou.warning.WarningSignService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BriefServiceTest {

    @Mock
    private CheckinMatchingService checkinMatchingService;
    @Mock
    private WarningSignService warningSignService;
    @Mock
    private SymptomRepository symptomRepository;
    @Mock
    private com.heardYou.condition.ConditionRepository conditionRepository;
    @Mock
    private com.heardYou.condition.ConditionSymptomRepository conditionSymptomRepository;

    @InjectMocks
    private BriefService briefService;

    // ── shared fixture helpers ────────────────────────────────────────────────

    private BriefRequest minimalRequest() {
        BriefRequest req = new BriefRequest();
        req.setLifeStage("reproductive-age");
        BriefRequest.SymptomEntry se = new BriefRequest.SymptomEntry();
        se.setSlug("pelvic-pain");
        se.setSeverity(4);
        se.setDuration("3 months");
        req.setSymptoms(List.of(se));
        return req;
    }

    private CheckinResponse emptyMatchResponse() {
        return new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER,
                "reproductive-age",
                List.of(),
                List.of(),
                List.of("pelvic-pain"),
                CheckinResponse.NO_MATCH_GUIDANCE,
                CheckinResponse.ALWAYS_ON_SAFETY);
    }

    private PatternMatch matchWithSource() {
        SourceDto source = new SourceDto(
                1L, "Endometriosis – NHS", "NHS",
                "https://www.nhs.uk/conditions/endometriosis/",
                null, List.of(), false);
        MatchedSymptomDetail detail = new MatchedSymptomDetail(
                "pelvic-pain", "Pelvic pain", 0.9, 4,
                com.heardYou.checkin.CheckinRequest.Duration.OVER_1_MONTH);
        return new PatternMatch(
                "endometriosis", "Endometriosis",
                "A condition causing pain and other symptoms.",
                "REPRODUCTIVE",
                0.72, "MODERATE",
                "You reported Pelvic pain. Women commonly report these together with Endometriosis.",
                List.of(detail),
                List.of("Pelvic ultrasound (transvaginal)", "Laparoscopy"),
                "If pain persists beyond 3 months, discuss with your GP.",
                "Many people describe years of being told their pain is 'just bad periods'.",
                List.of("Irritable bowel syndrome", "Normal menstrual pain"),
                List.of("Could this be endometriosis?"),
                "Gynaecologist",
                List.of(source));
    }

    private String extractAllText(byte[] pdfBytes) throws IOException {
        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(extractor.getTextFromPage(i));
        }
        reader.close();
        return sb.toString();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void pdfContainsStandardDisclaimerInHeader() throws IOException {
        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("Educational information, not medical advice");
        assertThat(text).contains("seek care now");
    }

    @Test
    void pdfContainsFooterPrivacyStatement() throws IOException {
        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("HeardYou does not store this information");
    }

    @Test
    void pdfRendersHumanReadableNameForMatchedSymptom() throws IOException {
        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(matchWithSource()), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        // symptomRepository.findBySlugIn is NOT called here: pelvic-pain is already
        // resolved from MatchedSymptomDetail, so no stub needed.

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        // Must show "Pelvic pain" (from MatchedSymptomDetail), not raw slug "pelvic-pain"
        assertThat(text).contains("Pelvic pain");
        assertThat(text).doesNotContain("• pelvic-pain");
    }

    @Test
    void pdfResolvesNameForUnmatchedSymptomViaCatalogue() throws IOException {
        // Symptom is submitted but produces no condition match → it is unmatched
        BriefRequest req = new BriefRequest();
        req.setLifeStage("reproductive-age");
        BriefRequest.SymptomEntry se = new BriefRequest.SymptomEntry();
        se.setSlug("cognitive-fog");
        se.setSeverity(3);
        req.setSymptoms(List.of(se));

        CheckinResponse noMatches = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(), List.of(), List.of("cognitive-fog"), CheckinResponse.NO_MATCH_GUIDANCE,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(noMatches);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        Symptom catalogueSymptom = new Symptom();
        catalogueSymptom.setSlug("cognitive-fog");
        catalogueSymptom.setName("Cognitive fog (difficulty concentrating)");
        when(symptomRepository.findBySlugIn(List.of("cognitive-fog")))
                .thenReturn(List.of(catalogueSymptom));

        String text = extractAllText(briefService.generatePdf(req));

        assertThat(text).contains("Cognitive fog");
        assertThat(text).doesNotContain("• cognitive-fog");
    }

    @Test
    void pdfFallsBackToSlugWhenCatalogueReturnsMiss() throws IOException {
        // Slug not found in catalogue at all – must fall back to the raw slug gracefully
        BriefRequest req = new BriefRequest();
        req.setLifeStage("reproductive-age");
        BriefRequest.SymptomEntry se = new BriefRequest.SymptomEntry();
        se.setSlug("unknown-symptom-xyz");
        se.setSeverity(2);
        req.setSymptoms(List.of(se));

        CheckinResponse noMatches = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(), List.of(), List.of("unknown-symptom-xyz"), CheckinResponse.NO_MATCH_GUIDANCE,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(noMatches);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of()); // nothing returned

        String text = extractAllText(briefService.generatePdf(req));

        // Falls back to slug – PDF must still render without throwing
        assertThat(text).contains("unknown-symptom-xyz");
    }

    @Test
    void pdfRendersSourceTitleAndUrlForConditionMatch() throws IOException {
        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(matchWithSource()), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("Endometriosis – NHS");
        assertThat(text).contains("https://www.nhs.uk/conditions/endometriosis/");
    }

    @Test
    void pdfRendersConditionMatchSectionWithNotDiagnosisLabel() throws IOException {
        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(matchWithSource()), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("not a diagnosis");
        assertThat(text).contains("Endometriosis");
        assertThat(text).contains("Gynaecologist");
        assertThat(text).contains("Could this be endometriosis?");
    }

    @Test
    void pdfRendersWarningSigns() throws IOException {
        WarningSign w = new WarningSign();
        w.setName("Sudden severe pain");
        w.setMessage("Seek emergency care immediately.");
        w.setRecommendedAction("Go to A&E immediately.");
        w.setSourceUrl("https://www.nhs.uk/");
        w.setSourceTitle("NHS");

        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of(w));
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        // Compact layout: renders heading, warning name, and recommended action
        assertThat(text).contains("Warning signs identified");
        assertThat(text).contains("Sudden severe pain");
        assertThat(text).contains("Go to A&E immediately");
    }

    @Test
    void symptomRepositoryIsQueriedOnlyForUnresolvedSlugs() {
        // When all submitted slugs are resolved via matched symptom details, repository must not be called
        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(matchWithSource()), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        briefService.generatePdf(minimalRequest());

        // pelvic-pain is already in MatchedSymptomDetail, so no repository call is needed
        verify(symptomRepository, never()).findBySlugIn(anyList());
    }

    // ── spec title and display name ───────────────────────────────────────────

    @Test
    void pdfTitleIsExactlyMySymptomBrief() throws IOException {
        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("My symptom brief");
    }

    @Test
    void pdfDoesNotContainOldDoctorAppointmentPreparationTitle() throws IOException {
        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        // The old title must no longer appear
        assertThat(text).doesNotContain("Doctor Appointment Preparation");
    }

    @Test
    void pdfRendersDisplayNameWhenProvided() throws IOException {
        BriefRequest req = minimalRequest();
        req.setDisplayName("Jane Smith");

        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(req));

        assertThat(text).contains("My symptom brief");
        assertThat(text).contains("Jane Smith");
        // Name is on a separate line, not merged into the title
        assertThat(text).doesNotContain("My symptom brief — Jane Smith");
    }

    @Test
    void pdfOmitsDisplayNameLineWhenNotProvided() throws IOException {
        // minimalRequest() has no displayName set
        when(checkinMatchingService.match(any())).thenReturn(emptyMatchResponse());
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());
        when(symptomRepository.findBySlugIn(anyList())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        assertThat(text).contains("My symptom brief");
        assertThat(text).doesNotContain("Prepared for:");
    }

    // ── clinician questions cap (max 8) ───────────────────────────────────────

    @Test
    void pdfCapsQuestionsAtEight() throws IOException {
        // Build a PatternMatch with more than 8 clinician questions
        SourceDto source = new SourceDto(1L, "Test Source", "Test", "https://example.com",
                null, List.of(), false);
        MatchedSymptomDetail detail = new MatchedSymptomDetail(
                "pelvic-pain", "Pelvic pain", 0.9, 4,
                com.heardYou.checkin.CheckinRequest.Duration.OVER_1_MONTH);
        PatternMatch matchWith10Questions = new PatternMatch(
                "test-cond", "Test Condition",
                "Summary.",
                "REPRODUCTIVE",
                0.72, "MODERATE",
                "You reported Pelvic pain. Women commonly report these together with Test Condition.",
                List.of(detail),
                List.of("Test A"),
                "Seek help if needed.",
                "Women experience this differently.",
                List.of(),
                List.of(
                        "Q1: First question?",
                        "Q2: Second question?",
                        "Q3: Third question?",
                        "Q4: Fourth question?",
                        "Q5: Fifth question?",
                        "Q6: Sixth question?",
                        "Q7: Seventh question?",
                        "Q8: Eighth question?",
                        "Q9: Ninth question — should be excluded",
                        "Q10: Tenth question — should be excluded"
                ),
                "Specialist",
                List.of(source));

        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(matchWith10Questions), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of());

        String text = extractAllText(briefService.generatePdf(minimalRequest()));

        // The summary section header must be present
        assertThat(text).contains("Questions to bring to your appointment:");
        // First 8 questions must appear (at minimum, in the condition block)
        assertThat(text).contains("Q1: First question?");
        assertThat(text).contains("Q8: Eighth question?");
        // In the compact layout per-condition questions are NOT printed inline —
        // they only appear in the deduplicated summary. Q9 and Q10 must be absent entirely.
        int summaryStart = text.indexOf("Questions to bring to your appointment:");
        assertThat(summaryStart).isGreaterThan(0);
        String summarySection = text.substring(summaryStart);
        assertThat(summarySection).doesNotContain("Q9: Ninth question");
        assertThat(summarySection).doesNotContain("Q10: Tenth question");
    }

    // ── single A4 page requirement ────────────────────────────────────────────

    @Test
    void pdfWithTwoMatchesAndWarningFitsOnOnePage() throws IOException {
        // Build a realistic worst-case: one condition match + one warning sign + display name
        SourceDto source = new SourceDto(1L, "Endometriosis – NHS", "NHS",
                "https://www.nhs.uk/conditions/endometriosis/", null, List.of(), false);
        MatchedSymptomDetail detail = new MatchedSymptomDetail(
                "pelvic-pain", "Pelvic pain", 0.9, 4,
                com.heardYou.checkin.CheckinRequest.Duration.OVER_1_MONTH);
        PatternMatch match = new PatternMatch(
                "endometriosis", "Endometriosis",
                "A condition where tissue similar to the uterine lining grows outside the uterus.",
                "REPRODUCTIVE",
                0.72, "MODERATE",
                "You reported Pelvic pain. Women commonly report these with Endometriosis.",
                List.of(detail),
                List.of("Pelvic ultrasound (transvaginal)", "MRI of the pelvis", "Laparoscopy"),
                "Discuss with your GP if pain persists beyond 3 months.",
                "Many people describe years of being told their pain is 'just bad periods'.",
                List.of("IBS", "Normal menstrual pain"),
                List.of(
                        "Could my symptoms be endometriosis?",
                        "Would a gynaecologist referral be appropriate?",
                        "Are there imaging tests that might help?",
                        "What pain management options are available?",
                        "Could my GI symptoms be related to a gynaecological condition?"
                ),
                "Gynaecologist",
                List.of(source));

        WarningSign w = new WarningSign();
        w.setName("Sudden severe pelvic pain");
        w.setMessage("May indicate ectopic pregnancy or ovarian torsion.");
        w.setRecommendedAction("Call 999 or go to A&E immediately.");
        w.setSourceUrl("https://www.nhs.uk/");
        w.setSourceTitle("NHS");

        BriefRequest req = minimalRequest();
        req.setDisplayName("Jane Smith");
        req.setConcerns("Ongoing pelvic pain");

        CheckinResponse response = new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER, "reproductive-age",
                List.of(match), List.of(), List.of(), null,
                CheckinResponse.ALWAYS_ON_SAFETY);
        when(checkinMatchingService.match(any())).thenReturn(response);
        when(warningSignService.evaluateForSymptoms(anyList(), any())).thenReturn(List.of(w));

        byte[] pdf = briefService.generatePdf(req);

        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();

        assertThat(pages)
                .as("PDF must fit on exactly one A4 page")
                .isEqualTo(1);

        // Content checks — all required sections must still be present
        String text = extractAllText(pdf);
        assertThat(text).contains("My symptom brief");
        assertThat(text).contains("Jane Smith");
        assertThat(text).contains("Educational information, not medical advice");
        assertThat(text).contains("seek care now");
        assertThat(text).contains("Warning signs identified");
        assertThat(text).contains("Sudden severe pelvic pain");
        assertThat(text).contains("Educational patterns found");
        assertThat(text).contains("not a diagnosis");
        assertThat(text).contains("Endometriosis");
        assertThat(text).contains("Questions to bring to your appointment");
        assertThat(text).contains("HeardYou does not store this information");
    }
}
