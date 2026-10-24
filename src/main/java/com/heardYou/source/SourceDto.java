package com.heardYou.source;

import java.time.LocalDate;
import java.util.List;

/** Read-only DTO for MedicalSource. */
public record SourceDto(
        Long id,
        String title,
        String publisher,
        String url,
        LocalDate reviewDate,
        List<String> supportedClaims,
        boolean needsEditorialReview
) {
    public static SourceDto from(MedicalSource s) {
        return new SourceDto(
                s.getId(), s.getTitle(), s.getPublisher(), s.getUrl(),
                s.getReviewDate(), s.getSupportedClaims(), s.isNeedsEditorialReview());
    }
}
