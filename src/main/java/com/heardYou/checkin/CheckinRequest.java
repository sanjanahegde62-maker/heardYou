package com.heardYou.checkin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Payload for POST /api/checkin. */
public class CheckinRequest {

    @NotNull(message = "lifeStage is required")
    @NotBlank(message = "lifeStage must not be blank")
    private String lifeStage;

    @NotNull(message = "symptoms list is required")
    @Size(min = 1, max = 15, message = "Between 1 and 15 symptoms must be provided")
    @Valid
    private List<SymptomEntry> symptoms;

    public String getLifeStage() { return lifeStage; }
    public void setLifeStage(String lifeStage) { this.lifeStage = lifeStage; }
    public List<SymptomEntry> getSymptoms() { return symptoms; }
    public void setSymptoms(List<SymptomEntry> symptoms) { this.symptoms = symptoms; }

    public static final java.util.Set<String> ALLOWED_LIFE_STAGES = java.util.Set.of(
            "teen", "reproductive-age", "perimenopause", "postmenopause", "adult");

    public enum Duration {
        UNDER_1_WEEK,
        ONE_TO_4_WEEKS,
        OVER_1_MONTH;

        @JsonCreator
        public static Duration fromString(String s) {
            if (s == null) return null;
            return switch (s.trim().toUpperCase()) {
                case "UNDER_1_WEEK"                -> UNDER_1_WEEK;
                case "1_TO_4_WEEKS", "ONE_TO_4_WEEKS" -> ONE_TO_4_WEEKS;
                case "OVER_1_MONTH"                -> OVER_1_MONTH;
                default -> throw new IllegalArgumentException(
                        "Invalid duration value '" + s + "'. Allowed: UNDER_1_WEEK, 1_TO_4_WEEKS, OVER_1_MONTH");
            };
        }

        @JsonValue
        public String toJsonValue() {
            return switch (this) {
                case UNDER_1_WEEK   -> "UNDER_1_WEEK";
                case ONE_TO_4_WEEKS -> "1_TO_4_WEEKS";
                case OVER_1_MONTH   -> "OVER_1_MONTH";
            };
        }

        public double factor() {
            return switch (this) {
                case UNDER_1_WEEK   -> 0.5;
                case ONE_TO_4_WEEKS -> 0.8;
                case OVER_1_MONTH   -> 1.0;
            };
        }
    }

    public static class SymptomEntry {

        @NotBlank(message = "symptom slug is required")
        private String slug;

        /** 1 = barely noticeable, 5 = severe. */
        @NotNull(message = "severity is required")
        @Min(value = 1, message = "severity must be at least 1")
        @Max(value = 5, message = "severity must be at most 5")
        private Integer severity;

        private Duration duration;

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public Integer getSeverity() { return severity; }
        public void setSeverity(Integer severity) { this.severity = severity; }
        public Duration getDuration() { return duration; }
        public void setDuration(Duration duration) { this.duration = duration; }
    }
}