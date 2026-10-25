package com.heardYou.brief;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Payload for POST /api/brief. Not persisted. */
public class BriefRequest {

    @NotBlank(message = "lifeStage is required")
    private String lifeStage;

    private String displayName;
    private String concerns;

    @NotNull(message = "symptoms list is required")
    @Size(min = 1, max = 15, message = "Between 1 and 15 symptoms must be provided")
    @Valid
    private List<SymptomEntry> symptoms;

    private List<String> conditionsOfInterest;
    private List<String> selectedSlugs;
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
    public void setConditionsOfInterest(List<String> c) { this.conditionsOfInterest = c; }
    public List<String> getSelectedSlugs() { return selectedSlugs; }
    public void setSelectedSlugs(List<String> s) { this.selectedSlugs = s; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String s) { this.additionalNotes = s; }

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