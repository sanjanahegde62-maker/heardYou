package com.heardYou.symptom;

/** Lightweight symptom summary used in lists and search results. */
public record SymptomSummaryDto(
        Long id,
        String name,
        String slug,
        String category
) {
    public static SymptomSummaryDto from(Symptom s) {
        return new SymptomSummaryDto(s.getId(), s.getName(), s.getSlug(), s.getCategory());
    }
}
