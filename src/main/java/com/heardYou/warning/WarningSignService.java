package com.heardYou.warning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WarningSignService {

    private final WarningSignRepository warningSignRepository;

    public WarningSignService(WarningSignRepository warningSignRepository) {
        this.warningSignRepository = warningSignRepository;
    }

    /**
     * Returns all warning signs triggered by the given symptom slugs,
     * respecting minMatches and requiredLifeStage constraints.
     *
     * Warning sign evaluation is ALWAYS independent of condition matching scores.
     * A low or zero match score must NEVER suppress a warning sign.
     *
     * @param symptomSlugs  slugs submitted in the check-in
     * @param lifeStage     the check-in life stage (may be null)
     */
    public List<WarningSign> evaluateForSymptoms(List<String> symptomSlugs, String lifeStage) {
        if (symptomSlugs == null || symptomSlugs.isEmpty()) {
            return List.of();
        }
        List<WarningSign> candidates = warningSignRepository.findByTriggerSymptomSlugIn(symptomSlugs);
        return candidates.stream()
                .filter(w -> passesLifeStageFilter(w, lifeStage))
                .filter(w -> passesMinMatchesFilter(w, symptomSlugs))
                .toList();
    }

    /**
     * Overload without life-stage for backward compatibility with tests that don't pass one.
     */
    public List<WarningSign> evaluateForSymptoms(List<String> symptomSlugs) {
        return evaluateForSymptoms(symptomSlugs, null);
    }

    private boolean passesLifeStageFilter(WarningSign w, String lifeStage) {
        // null requiredLifeStage means "all life stages"
        return w.getRequiredLifeStage() == null
                || w.getRequiredLifeStage().equalsIgnoreCase(lifeStage);
    }

    private boolean passesMinMatchesFilter(WarningSign w, List<String> submittedSlugs) {
        long matches = w.getTriggerSymptomSlugs().stream()
                .filter(submittedSlugs::contains)
                .count();
        return matches >= w.getMinMatches();
    }
}
