package com.heardYou.brief;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Payload for POST /api/brief. Not persisted. */
public class BriefRequest {

    @NotBlank(message = "lifeStage is required")
    private String lifeStage;

    /**
     * Optional display name for the document header.
     * Never stored or logged — used only for PDF generation in this request cycle.
     */
    private String displayName;

    /** Free-text concerns the person wants to bring to their appointment. */
    private String concerns;

    @NotNull(message = "symptoms list is required")
    @Size(min = 1, max = 15, message = "Between 1 and 15 symptoms must be provided")
    @Valid
    private List<SymptomEntry> symptoms;

    /** Optional: condition slugs the person wants more information about. */
    private List<String> conditionsOfInterest;

    /**
     * Optional: condition slugs selected from the catalogue for inclusion in the brief.
     * Supplements the matching algorithm with user-curated conditions.
     */
    private List<String> selectedSlugs;

    /** Optional: additional notes for the appointment. */
    private String additionalNotes;

    public String getLifeStage() { return lifeStage; }
    public void setLifeStage(String lifeStage) { this.lifeStage = lifeStage; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getConcerns() { return concerns; }
    public void setConcerns(String concerns) { this.concerns = concerns; }
    public List<SymptomEntry> getSymptoms() { return symptoms; }
    public void setSymptoms(List<SymptomEntry> symptoms) { this.symptoms = symptoms; }
    public List<String> getConditionsOfInterest() { return conditionsOfInterest; }
    public void setConditionsOfInterest(List<String> conditionsOfInterest) { this.conditionsOfInterest = conditionsOfInterest; }
    public List<String> getSelectedSlugs() { return selectedSlugs; }
    public void setSelectedSlugs(List<String> selectedSlugs) { this.selectedSlugs = selectedSlugs; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public static class SymptomEntry {
        @NotBlank(message = "symptom slug is required")
        private String slug;

        @NotNull @Min(1) @Max(5)
        private Integer severity;

        private String duration;

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public Integer getSeverity() { return severity; }
        public void setSeverity(Integer severity) { this.severity = severity; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }
}
