package com.heardYou.checkin;

import com.heardYou.source.SourceDto;
import com.heardYou.warning.WarningSign;

import java.util.List;


public record CheckinResponse(
        String disclaimer,
        String lifeStage,
        List<PatternMatch> matches,
        List<WarningSignDto> warningSignsEvaluated,
        List<String> unmatchedSymptomSlugs,
        
        String noMatchGuidance,
        
        String alwaysOnSafety
) {
    /**
     * A single educational pattern match.
     *
     * {@code educationalOverlapScore} is a transparent, deterministic score between
     * 0.0 and 1.0. It is NOT a disease likelihood, probability, or clinical assessment.
     *
     * {@code overlapLabel} is a plain-English tier:
     *   HIGH     ≥ 0.60
     *   MODERATE ≥ 0.35 and < 0.60
     *   LOW      < 0.35
     * These labels describe documented educational overlap only.
     */
    public record PatternMatch(
            String conditionSlug,
            String conditionName,
            String conditionSummary,
            String bodySystem,
            double educationalOverlapScore,
            String overlapLabel,
            String overlapExplanation,
            List<MatchedSymptomDetail> matchedSymptoms,
            /**
             * Suggested tests from the condition catalogue — not a clinical recommendation.
             * For educational use only; the doctor decides what is appropriate.
             */
            List<String> suggestedTests,
            /**
             * Timeframe advice from the condition catalogue — when to seek help.
             * Not a diagnosis. Not a safety assessment.
             */
            String timeframeAdvice,
            /** "Textbook vs. Her" — how women actually experience this condition. */
            String femalePresentation,
            List<String> commonlyMistakenFor,
            List<String> clinicianQuestions,
            String specialistType,
            List<SourceDto> sources
    ) {}

    public record MatchedSymptomDetail(
            String symptomSlug,
            String symptomName,
            double editorialWeight,
            int submittedSeverity,
            CheckinRequest.Duration submittedDuration
    ) {}

    public record WarningSignDto(
            String name,
            String message,
            String recommendedAction,
            String sourceUrl,
            String sourceTitle
    ) {
        public static WarningSignDto from(WarningSign w) {
            return new WarningSignDto(
                    w.getName(), w.getMessage(), w.getRecommendedAction(),
                    w.getSourceUrl(), w.getSourceTitle());
        }
    }

    public static final String STANDARD_DISCLAIMER =
            "Educational information, not medical advice. " +
            "If you're worried or something feels very wrong, seek care now. " +
            "In an emergency, call your local emergency number (112 in India).";

    /**
     * Always-on safety line included in every response regardless of match results.
     * The user must never be told they are "fine" or "safe" based on algorithm output.
     */
    public static final String ALWAYS_ON_SAFETY =
            "Feeling worried or something feels very wrong? Don't wait for this app. Seek care.";

    /**
     * Guidance returned in the {@code noMatchGuidance} field when the algorithm
     * finds no educational pattern overlap above the threshold.
     *
     * A no-match result must never be treated as reassurance. The user's symptoms
     * are real and deserve medical attention regardless of what this tool finds.
     */
    public static final String NO_MATCH_GUIDANCE =
            "No documented educational pattern overlap was found above the matching threshold " +
            "for the symptoms you described. This does not mean your symptoms are not important " +
            "or that nothing is wrong. Symptoms that are new, persistent, or affecting your daily " +
            "life are always worth discussing with a doctor or healthcare professional.";

    /**
     * Safety line returned when no warning signs are triggered.
     * Per specification: "No known warning signs matched. This doesn't mean nothing is wrong."
     * Must never be used to reassure the user.
     */
    public static final String NO_WARNING_SIGNS_GUIDANCE =
            "No known warning signs matched. This doesn't mean nothing is wrong.";
}
