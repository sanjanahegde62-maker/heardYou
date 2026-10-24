package com.heardYou.brief;

import com.heardYou.checkin.CheckinMatchingService;
import com.heardYou.checkin.CheckinRequest;
import com.heardYou.checkin.CheckinResponse;
import com.heardYou.checkin.CheckinResponse.MatchedSymptomDetail;
import com.heardYou.checkin.CheckinResponse.PatternMatch;
import com.heardYou.condition.Condition;
import com.heardYou.condition.ConditionRepository;
import com.heardYou.condition.ConditionSymptom;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.source.SourceDto;
import com.heardYou.symptom.Symptom;
import com.heardYou.symptom.SymptomRepository;
import com.heardYou.warning.WarningSign;
import com.heardYou.warning.WarningSignService;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BriefService {

    /** Spec: maximum symptoms shown in the PDF. */
    private static final int MAX_SYMPTOMS = 5;
    /** Spec: maximum deduplicated questions in the summary section. */
    private static final int MAX_QUESTIONS = 8;
    /** Max characters of a condition summary before truncation with "…". */
    private static final int MAX_SUMMARY_CHARS = 130;
    /** Max per-condition tests shown inline. */
    private static final int MAX_TESTS_PER_CONDITION = 3;

    private final CheckinMatchingService checkinMatchingService;
    private final WarningSignService warningSignService;
    private final SymptomRepository symptomRepository;
    private final ConditionRepository conditionRepository;
    private final ConditionSymptomRepository conditionSymptomRepository;

    public BriefService(CheckinMatchingService checkinMatchingService,
                         WarningSignService warningSignService,
                         SymptomRepository symptomRepository,
                         ConditionRepository conditionRepository,
                         ConditionSymptomRepository conditionSymptomRepository) {
        this.checkinMatchingService = checkinMatchingService;
        this.warningSignService = warningSignService;
        this.symptomRepository = symptomRepository;
        this.conditionRepository = conditionRepository;
        this.conditionSymptomRepository = conditionSymptomRepository;
    }

    public byte[] generatePdf(BriefRequest briefRequest) {

        CheckinRequest checkinRequest = toCheckinRequest(briefRequest);
        CheckinResponse matchResult = checkinMatchingService.match(checkinRequest);

        List<WarningSign> warnings = warningSignService.evaluateForSymptoms(
                briefRequest.getSymptoms().stream().map(BriefRequest.SymptomEntry::getSlug).toList(),
                briefRequest.getLifeStage());

        // Build slug→name map from matched symptom details first
        Map<String, String> slugToName = new HashMap<>();
        for (PatternMatch pm : matchResult.matches()) {
            for (MatchedSymptomDetail d : pm.matchedSymptoms()) {
                slugToName.put(d.symptomSlug(), d.symptomName());
            }
        }
        // Resolve any slugs not covered by match results via the catalogue
        List<String> unresolvedSlugs = briefRequest.getSymptoms().stream()
                .map(BriefRequest.SymptomEntry::getSlug)
                .filter(s -> !slugToName.containsKey(s))
                .toList();
        if (!unresolvedSlugs.isEmpty()) {
            symptomRepository.findBySlugIn(unresolvedSlugs).forEach(
                    s -> slugToName.put(s.getSlug(), s.getName()));
        }

        // Merge algorithm matches + user-selected conditions (no duplicates)
        List<PatternMatch> selectedMatches = resolveSelectedSlugs(briefRequest, matchResult);
        List<PatternMatch> allMatches = new ArrayList<>(matchResult.matches());
        for (PatternMatch sm : selectedMatches) {
            boolean alreadyPresent = allMatches.stream()
                    .anyMatch(m -> m.conditionSlug().equals(sm.conditionSlug()));
            if (!alreadyPresent) {
                allMatches.add(sm);
            }
        }

        // Symptom list capped at MAX_SYMPTOMS per spec
        List<BriefRequest.SymptomEntry> symptoms = briefRequest.getSymptoms().stream()
                .limit(MAX_SYMPTOMS)
                .toList();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Reduced margins for compact single-page layout: left/right 36pt, top 45pt, bottom 36pt
            Document doc = new Document(PageSize.A4, 36, 36, 45, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Font sizes tuned for single-page fit
            Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font subheadFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font bodyFont    = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallFont   = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font boldBody    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            // ── Header ──────────────────────────────────────────────────────────
            Paragraph titlePara = new Paragraph("My symptom brief", titleFont);
            titlePara.setSpacingAfter(1);
            doc.add(titlePara);

            if (notBlank(briefRequest.getDisplayName())) {
                Paragraph namePara = new Paragraph("Prepared for: " + briefRequest.getDisplayName(), boldBody);
                namePara.setSpacingAfter(1);
                doc.add(namePara);
            }

            Paragraph datePara = new Paragraph(
                    "Generated: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), smallFont);
            datePara.setSpacingAfter(3);
            doc.add(datePara);

            Paragraph disclaimer = new Paragraph(CheckinResponse.STANDARD_DISCLAIMER, smallFont);
            disclaimer.setSpacingAfter(4);
            doc.add(disclaimer);
            addRule(doc);

            // ── Context ─────────────────────────────────────────────────────────
            // Life stage + optional concerns on a single line each, no extra blank lines
            Paragraph lsPara = new Paragraph("Life stage: " + briefRequest.getLifeStage(), boldBody);
            lsPara.setSpacingAfter(2);
            doc.add(lsPara);

            if (notBlank(briefRequest.getConcerns())) {
                Paragraph concernPara = new Paragraph("Concerns: " + briefRequest.getConcerns(), bodyFont);
                concernPara.setSpacingAfter(2);
                doc.add(concernPara);
            }

            // ── Symptoms ────────────────────────────────────────────────────────
            Paragraph sympHeader = new Paragraph("Symptoms to discuss:", headingFont);
            sympHeader.setSpacingAfter(1);
            doc.add(sympHeader);

            for (BriefRequest.SymptomEntry se : symptoms) {
                String displayName = slugToName.getOrDefault(se.getSlug(), se.getSlug());
                String durationText = notBlank(se.getDuration()) ? ", " + se.getDuration() : "";
                Paragraph sp = new Paragraph(
                        "  \u2022 " + displayName + " (sev " + se.getSeverity() + "/5" + durationText + ")",
                        bodyFont);
                sp.setSpacingAfter(1);
                doc.add(sp);
            }
            if (briefRequest.getSymptoms().size() > MAX_SYMPTOMS) {
                Paragraph more = new Paragraph(
                        "  (+" + (briefRequest.getSymptoms().size() - MAX_SYMPTOMS) + " more — discuss with doctor)",
                        smallFont);
                more.setSpacingAfter(1);
                doc.add(more);
            }

            // ── Warning Signs ────────────────────────────────────────────────────
            if (!warnings.isEmpty()) {
                addRule(doc);
                Paragraph wHeader = new Paragraph("Warning signs identified:", headingFont);
                wHeader.setSpacingAfter(2);
                doc.add(wHeader);
                for (WarningSign w : warnings) {
                    // Name bold, then action on same element as one paragraph each
                    Paragraph wLine = new Paragraph("\u26A0 " + w.getName(), boldBody);
                    wLine.setSpacingAfter(1);
                    doc.add(wLine);
                    if (notBlank(w.getRecommendedAction())) {
                        Paragraph wAction = new Paragraph("  Action: " + w.getRecommendedAction(), smallFont);
                        wAction.setSpacingAfter(1);
                        doc.add(wAction);
                    }
                    if (notBlank(w.getSourceUrl())) {
                        Paragraph wSrc = new Paragraph(
                                "  Source: " + w.getSourceTitle() + " \u2014 " + w.getSourceUrl(), smallFont);
                        wSrc.setSpacingAfter(2);
                        doc.add(wSrc);
                    }
                }
            }

            // ── Educational Pattern Matches ───────────────────────────────────────
            addRule(doc);
            if (!allMatches.isEmpty()) {
                Paragraph matchHeader = new Paragraph(
                        "Educational patterns found (not a diagnosis):", headingFont);
                matchHeader.setSpacingAfter(1);
                doc.add(matchHeader);

                Paragraph matchIntro = new Paragraph(
                        "Documented educational overlap with your symptoms. Not a diagnosis. "
                        + "Share with your doctor.", smallFont);
                matchIntro.setSpacingAfter(3);
                doc.add(matchIntro);

                Set<String> seenQuestions = new LinkedHashSet<>();

                int rank = 1;
                for (PatternMatch match : allMatches) {
                    // Condition name line
                    Paragraph condName = new Paragraph(
                            rank++ + ". " + match.conditionName()
                            + (notBlank(match.bodySystem()) ? "  [" + match.bodySystem() + "]" : ""),
                            boldBody);
                    condName.setSpacingAfter(1);
                    doc.add(condName);

                    // Truncated summary
                    String summary = truncate(match.conditionSummary(), MAX_SUMMARY_CHARS);
                    Paragraph sumPara = new Paragraph(summary, bodyFont);
                    sumPara.setSpacingAfter(1);
                    doc.add(sumPara);

                    // Overlap score in one line
                    Paragraph overlapLine = new Paragraph(
                            "Overlap: " + String.format("%.0f%%", match.educationalOverlapScore() * 100)
                            + " (" + match.overlapLabel() + ") \u2014 " + match.overlapExplanation(),
                            smallFont);
                    overlapLine.setSpacingAfter(1);
                    doc.add(overlapLine);

                    // Timeframe on one line
                    if (notBlank(match.timeframeAdvice())) {
                        Paragraph tf = new Paragraph(
                                "When to seek help: " + truncate(match.timeframeAdvice(), 100), smallFont);
                        tf.setSpacingAfter(1);
                        doc.add(tf);
                    }

                    // Tests: comma-joined, capped at MAX_TESTS_PER_CONDITION
                    if (match.suggestedTests() != null && !match.suggestedTests().isEmpty()) {
                        List<String> tests = match.suggestedTests().stream()
                                .limit(MAX_TESTS_PER_CONDITION).toList();
                        String testLine = "Tests: " + String.join(", ", tests);
                        if (match.suggestedTests().size() > MAX_TESTS_PER_CONDITION) {
                            testLine += " (+ more)";
                        }
                        Paragraph tp = new Paragraph(testLine, smallFont);
                        tp.setSpacingAfter(1);
                        doc.add(tp);
                    }

                    // Specialist type
                    if (notBlank(match.specialistType())) {
                        Paragraph sp = new Paragraph("Specialist: " + match.specialistType(), smallFont);
                        sp.setSpacingAfter(1);
                        doc.add(sp);
                    }

                    // Collect questions for the summary — do NOT print them per-condition
                    if (match.clinicianQuestions() != null) {
                        seenQuestions.addAll(match.clinicianQuestions());
                    }

                    // Sources: title only (no URL per-condition to save space)
                    if (!match.sources().isEmpty()) {
                        String srcLine = "Sources: " + match.sources().stream()
                                .map(SourceDto::title)
                                .collect(Collectors.joining(", "));
                        // Keep URL for first source only
                        SourceDto first = match.sources().get(0);
                        if (notBlank(first.url())) {
                            srcLine += " \u2014 " + first.url();
                        }
                        Paragraph srcp = new Paragraph(srcLine, smallFont);
                        srcp.setSpacingAfter(3);
                        doc.add(srcp);
                    }
                }

                // ── Deduplicated questions summary — capped at MAX_QUESTIONS ───────
                if (!seenQuestions.isEmpty()) {
                    addRule(doc);
                    Paragraph qHeader = new Paragraph(
                            "Questions to bring to your appointment:", headingFont);
                    qHeader.setSpacingAfter(2);
                    doc.add(qHeader);
                    int qCount = 0;
                    for (String q : seenQuestions) {
                        if (qCount >= MAX_QUESTIONS) break;
                        Paragraph qp = new Paragraph("  \u2022 " + q, bodyFont);
                        qp.setSpacingAfter(1);
                        doc.add(qp);
                        qCount++;
                    }
                }

            } else {
                // No matches
                Paragraph noMatch = new Paragraph("No educational pattern match found", headingFont);
                noMatch.setSpacingAfter(2);
                doc.add(noMatch);
                Paragraph nmGuide = new Paragraph(CheckinResponse.NO_MATCH_GUIDANCE, bodyFont);
                nmGuide.setSpacingAfter(2);
                doc.add(nmGuide);
            }

            // ── Additional notes ─────────────────────────────────────────────────
            if (notBlank(briefRequest.getAdditionalNotes())) {
                addRule(doc);
                Paragraph notesHeader = new Paragraph("Additional notes:", headingFont);
                notesHeader.setSpacingAfter(1);
                doc.add(notesHeader);
                Paragraph notesPara = new Paragraph(briefRequest.getAdditionalNotes(), bodyFont);
                notesPara.setSpacingAfter(2);
                doc.add(notesPara);
            }

            // ── Footer disclaimer ────────────────────────────────────────────────
            addRule(doc);
            Paragraph footer = new Paragraph(
                    "This document was generated for personal use to support a medical appointment. "
                    + "It is not a medical record and does not replace professional medical advice. "
                    + "HeardYou does not store this information.", smallFont);
            footer.setSpacingAfter(0);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    /**
     * Resolves user-selected condition slugs into PatternMatch summaries for the PDF.
     * These are conditions the user explicitly chose from the catalogue to include,
     * independent of the algorithm's output. They use null/zero overlap scores
     * since they were not matched algorithmically.
     */
    private List<PatternMatch> resolveSelectedSlugs(BriefRequest briefRequest, CheckinResponse matchResult) {
        List<String> selectedSlugs = briefRequest.getSelectedSlugs();
        if (selectedSlugs == null || selectedSlugs.isEmpty()) {
            return List.of();
        }
        Set<String> alreadyMatched = matchResult.matches().stream()
                .map(PatternMatch::conditionSlug)
                .collect(Collectors.toSet());

        List<String> toResolve = selectedSlugs.stream()
                .filter(s -> !alreadyMatched.contains(s))
                .toList();
        if (toResolve.isEmpty()) return List.of();

        return conditionRepository.findBySlugIn(toResolve).stream()
                .map(c -> {
                    List<ConditionSymptom> mappings = conditionSymptomRepository.findByCondition(c);
                    List<SourceDto> sources = c.getSources().stream().map(SourceDto::from).toList();
                    return new PatternMatch(
                            c.getSlug(), c.getName(), c.getSummary(),
                            c.getBodySystem(),
                            0.0, "SELECTED",
                            "You selected this condition from the catalogue to discuss with your doctor.",
                            List.of(),
                            c.getSuggestedTests() != null ? c.getSuggestedTests() : List.of(),
                            c.getTimeframeAdvice(),
                            c.getFemalePresentation(),
                            c.getCommonlyMistakenFor() != null ? c.getCommonlyMistakenFor() : List.of(),
                            c.getClinicianQuestions(),
                            c.getSpecialistType(),
                            sources);
                })
                .toList();
    }

    private CheckinRequest toCheckinRequest(BriefRequest briefRequest) {
        CheckinRequest cr = new CheckinRequest();
        cr.setLifeStage(briefRequest.getLifeStage());
        List<CheckinRequest.SymptomEntry> entries = briefRequest.getSymptoms().stream()
                .map(se -> {
                    CheckinRequest.SymptomEntry entry = new CheckinRequest.SymptomEntry();
                    entry.setSlug(se.getSlug());
                    entry.setSeverity(se.getSeverity());
                    CheckinRequest.Duration parsedDuration = null;
                    if (se.getDuration() != null && !se.getDuration().isBlank()) {
                        try {
                            parsedDuration = CheckinRequest.Duration.fromString(se.getDuration());
                        } catch (IllegalArgumentException ignored) {
                            // Unrecognised free-text → treated as omitted
                        }
                    }
                    entry.setDuration(parsedDuration);
                    return entry;
                }).toList();
        cr.setSymptoms(entries);
        return cr;
    }

    private void addRule(Document doc) throws DocumentException {
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(new java.awt.Color(200, 200, 200));
        ls.setLineWidth(0.5f);
        Chunk rule = new Chunk(ls);
        Paragraph rulePara = new Paragraph();
        rulePara.add(rule);
        rulePara.setSpacingBefore(2);
        rulePara.setSpacingAfter(3);
        doc.add(rulePara);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
