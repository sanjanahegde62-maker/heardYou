package com.heardYou.symptom;

import java.util.List;

/** Full symptom detail. */
public record SymptomDetailDto(
        Long id,
        String name,
        String slug,
        String description,
        String category,
        List<String> aliases
) {
    public static SymptomDetailDto from(Symptom s) {
        return new SymptomDetailDto(
                s.getId(), s.getName(), s.getSlug(), s.getDescription(), s.getCategory(),
                s.getAliases() != null ? s.getAliases() : List.of());
    }
}
