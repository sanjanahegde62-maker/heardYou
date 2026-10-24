package com.heardYou.checkin;

import com.heardYou.checkin.CheckinRequest.Duration;
import com.heardYou.checkin.CheckinRequest.SymptomEntry;
import com.heardYou.checkin.CheckinResponse.*;
import com.heardYou.common.ValidationException;
import com.heardYou.condition.Condition;
import com.heardYou.condition.ConditionSymptom;
import com.heardYou.condition.ConditionSymptomRepository;
import com.heardYou.source.SourceDto;
import com.heardYou.warning.WarningSignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic educational matching engine.
 *
 * Algorithm (from spec pp. 5–6):
 *
 *   For each condition C and each symptom s she reports that C lists:
 *   1. severityFactor  = 0.5 + 0.1 × severity          (severity 1→0.6, 5→1.0)
 *   2. durationFactor  = 0.5 | 0.8 | 1.0 per enum value; null → 0.8 (neutral)
 *   3. raw             = Σ weight(C, s) × severityFactor × durationFactor
 *   4. denominator     = sum of the 6 HIGHEST weights for C
 *   5. score           = min(1, raw / denominator)
 *                        + 0.10 if C is tagged for her life stage, capped at 1
 *   6. Include C only if matchedCount ≥ 2 AND score ≥ 0.20
 *   7. Overlap label   HIGH ≥ 0.60 | MODERATE ≥ 0.35 | LOW < 0.35
 *   8. Sort by score DESC, then matchedCount DESC; return top 5.
 *   9. "why" template: "You reported {labels}. Women commonly report these together
 *                        with {condition name}."
 *
 * Scores are NOT disease likelihoods, probabilities, or clinical assessments.
 * This algorithm is an editorial estimate, not a clinical measurement.
 */
@Service
@Transactional(readOnly = true)
public class CheckinMatchingService {

    /** Conditions with fewer than this many matched symptoms are excluded. */
    private static final int MIN_MATCHED_SYMPTOMS = 2;

    /** Conditions whose final score falls below this are excluded. */
    private static final double MIN_SCORE = 0.20;

    /** Maximum number of results returned. */
    private static final int MAX_RESULTS = 5;

    /**
     * Life-stage bonus: added when the condition is tagged for the submitted stage.
     * Capped so total score never exceeds 1.0.
     */
    private static final double LIFE_STAGE_BONUS = 0.10;

    /**
     * Neutral duration factor used when the user omitted duration for a symptom.
     * Missing data is not treated as absence and must not penalise the score.
     */
    private static final double NULL_DURATION_FACTOR = 0.8;

    private final ConditionSymptomRepository conditionSymptomRepository;
    private final WarningSignService warningSignService;

    public CheckinMatchingService(ConditionSymptomRepository conditionSymptomRepository,
                                   WarningSignService warningSignService) {
        this.conditionSymptomRepository = conditionSymptomRepository;
        this.warningSignService = warningSignService;
    }

    public CheckinResponse match(CheckinRequest request) {
        List<SymptomEntry> entries = request.getSymptoms();

        // Validate: life stage must be one of the allowed values
        String lifeStage = request.getLifeStage();
        if (lifeStage != null && !CheckinRequest.ALLOWED_LIFE_STAGES.contains(lifeStage)) {
            throw new com.heardYou.common.ValidationException(
                    "Invalid lifeStage '" + lifeStage + "'. Allowed values: " +
                    String.join(", ", CheckinRequest.ALLOWED_LIFE_STAGES.stream().sorted().toList()));
        }

        // Validate: no duplicate slugs
        List<String> slugs = entries.stream().map(SymptomEntry::getSlug).toList();
        Set<String> seen = new HashSet<>();
        List<String> duplicates = slugs.stream()
                .filter(s -> !seen.add(s))
                .distinct()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new ValidationException("Duplicate symptom slugs are not allowed: " + duplicates);
        }

        // slug → SymptomEntry lookup
        Map<String, SymptomEntry> entryBySlug = entries.stream()
                .collect(Collectors.toMap(SymptomEntry::getSlug, e -> e));

        // Fetch all condition-symptom mappings for the submitted slugs
        List<ConditionSymptom> mappings = conditionSymptomRepository.findBySymptomSlugIn(slugs);

        // Group by condition object (identity-based)
        Map<Condition, List<ConditionSymptom>> byCondition = mappings.stream()
                .collect(Collectors.groupingBy(ConditionSymptom::getCondition,
                        LinkedHashMap::new, Collectors.toList()));

        List<PatternMatch> matches = new ArrayList<>();
        Set<String> matchedSlugsGlobal = new HashSet<>();

        for (Map.Entry<Condition, List<ConditionSymptom>> entry : byCondition.entrySet()) {
            var condition = entry.getKey();
            List<ConditionSymptom> condMappings = entry.getValue();

            // ── step 3: raw score ────────────────────────────────────────────
            List<MatchedSymptomDetail> matchedDetails = new ArrayList<>();
            double raw = 0.0;

            for (ConditionSymptom cs : condMappings) {
                String slug = cs.getSymptom().getSlug();
                SymptomEntry submitted = entryBySlug.get(slug);
                if (submitted == null) continue;

                double severityFactor = 0.5 + 0.1 * submitted.getSeverity(); // 0.6–1.0
                double durationFactor = submitted.getDuration() != null
                        ? submitted.getDuration().factor()
                        : NULL_DURATION_FACTOR;

                raw += cs.getOverlapWeight() * severityFactor * durationFactor;

                matchedDetails.add(new MatchedSymptomDetail(
                        slug,
                        cs.getSymptom().getName(),
                        cs.getOverlapWeight(),
                        submitted.getSeverity(),
                        submitted.getDuration()));
                matchedSlugsGlobal.add(slug);
            }

            // ── step 6: minimum matched symptoms ────────────────────────────
            if (matchedDetails.size() < MIN_MATCHED_SYMPTOMS) continue;

            // ── step 4: denominator = sum of the 6 highest weights ───────────
            List<ConditionSymptom> allForCondition =
                    conditionSymptomRepository.findByCondition(condition);
            double denominator = allForCondition.stream()
                    .mapToDouble(ConditionSymptom::getOverlapWeight)
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .limit(6)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            if (denominator == 0) continue;

            // ── step 5: score, life-stage bonus, cap ────────────────────────
            double score = Math.min(1.0, raw / denominator);
            if (condition.getLifeStages().contains(request.getLifeStage())) {
                score = Math.min(1.0, score + LIFE_STAGE_BONUS);
            }

            // ── step 6: score threshold ──────────────────────────────────────
            if (score < MIN_SCORE) continue;

            // ── step 7: overlap label ─────────────────────────────────────────
            String overlapLabel = overlapLabel(score);

            // ── step 9: "why" sentence ────────────────────────────────────────
            String why = buildWhy(matchedDetails, condition.getName());

            List<SourceDto> sourceDtos = condition.getSources().stream()
                    .map(SourceDto::from)
                    .toList();

            // Round score to 3 decimal places for deterministic display
            double displayScore = Math.round(score * 1000.0) / 1000.0;

            matches.add(new PatternMatch(
                    condition.getSlug(),
                    condition.getName(),
                    condition.getSummary(),
                    condition.getBodySystem(),
                    displayScore,
                    overlapLabel,
                    why,
                    matchedDetails,
                    condition.getSuggestedTests() != null ? condition.getSuggestedTests() : List.of(),
                    condition.getTimeframeAdvice(),
                    condition.getFemalePresentation(),
                    condition.getCommonlyMistakenFor() != null ? condition.getCommonlyMistakenFor() : List.of(),
                    condition.getClinicianQuestions(),
                    condition.getSpecialistType(),
                    sourceDtos));
        }

        // ── step 8: sort and cap ──────────────────────────────────────────────
        matches.sort(Comparator
                .comparingDouble(PatternMatch::educationalOverlapScore).reversed()
                .thenComparingInt((PatternMatch m) -> m.matchedSymptoms().size()).reversed());
        if (matches.size() > MAX_RESULTS) {
            matches = matches.subList(0, MAX_RESULTS);
        }

        // Unmatched slugs (those that did not contribute to any condition result)
        List<String> unmatched = slugs.stream()
                .filter(s -> !matchedSlugsGlobal.contains(s))
                .toList();

        // Warning signs evaluated independently of pattern scores
        List<WarningSignDto> warningSigns = warningSignService.evaluateForSymptoms(slugs, request.getLifeStage())
                .stream()
                .map(WarningSignDto::from)
                .toList();

        // No-match guidance: always set; a no-match is never a reassurance
        String noMatchGuidance = matches.isEmpty() ? CheckinResponse.NO_MATCH_GUIDANCE : null;

        return new CheckinResponse(
                CheckinResponse.STANDARD_DISCLAIMER,
                request.getLifeStage(),
                matches,
                warningSigns,
                unmatched,
                noMatchGuidance,
                CheckinResponse.ALWAYS_ON_SAFETY);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the educational overlap label for a given score.
     * These are descriptive tiers, NOT disease probabilities.
     */
    static String overlapLabel(double score) {
        if (score >= 0.60) return "HIGH";
        if (score >= 0.35) return "MODERATE";
        return "LOW";
    }

    /**
     * Builds the "why" explanation sentence from the spec template:
     * "You reported {matched symptom labels}. Women commonly report these together
     *  with {condition name}."
     */
    static String buildWhy(List<MatchedSymptomDetail> matched, String conditionName) {
        List<String> names = matched.stream()
                .map(MatchedSymptomDetail::symptomName)
                .toList();
        String labelList = formatList(names);
        return "You reported " + labelList + ". Women commonly report these together with " + conditionName + ".";
    }

    /** Formats a list as "a, b and c" (Oxford-comma-free, matching natural English). */
    private static String formatList(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1))
                + " and " + items.get(items.size() - 1);
    }
}
