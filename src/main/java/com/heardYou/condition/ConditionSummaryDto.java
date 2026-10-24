package com.heardYou.condition;

import com.heardYou.source.SourceDto;

import java.util.List;
import java.util.Set;

/** Summary row for condition catalogue listings. */
public record ConditionSummaryDto(
        Long id,
        String name,
        String slug,
        String summary,
        String bodySystem,
        String category,
        Set<String> extraCategories,
        Set<String> lifeStages,
        String specialistType,
        String avgDiagnosisDelay
) {
    public static ConditionSummaryDto from(Condition c) {
        return new ConditionSummaryDto(
                c.getId(), c.getName(), c.getSlug(), c.getSummary(),
                c.getBodySystem(), c.getCategory(), c.getExtraCategories(),
                c.getLifeStages(), c.getSpecialistType(), c.getAvgDiagnosisDelay());
    }
}
