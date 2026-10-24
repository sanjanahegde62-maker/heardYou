package com.heardYou.warning;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An independently evaluated warning sign.
 *
 * Warning signs are evaluated regardless of matching scores.
 * A low or zero match score must NEVER suppress a warning sign.
 */
@Entity
@Table(name = "warning_signs")
public class WarningSign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String name;

    /** Guidance message shown to the user. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Action recommended (e.g. "Seek emergency care immediately"). */
    @Column(length = 300)
    private String recommendedAction;

    /** Symptom slugs that can trigger evaluation of this warning. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "warning_sign_symptom_slugs",
                     joinColumns = @JoinColumn(name = "warning_sign_id"))
    @Column(name = "symptom_slug", length = 200)
    private List<String> triggerSymptomSlugs = new ArrayList<>();

    /** Condition slugs this warning is associated with. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "warning_sign_condition_slugs",
                     joinColumns = @JoinColumn(name = "warning_sign_id"))
    @Column(name = "condition_slug", length = 200)
    private List<String> relatedConditionSlugs = new ArrayList<>();

    /** Authoritative source URL for this warning guidance. */
    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 500)
    private String sourceTitle;

    /**
     * Minimum number of matching trigger symptoms required before this warning is shown.
     * Use 1 (default) for warnings that fire on any single matching symptom.
     * Use a higher value for rules that require co-occurring symptoms (e.g. heart-attack signs).
     */
    @Column(nullable = false)
    private int minMatches = 1;

    /**
     * Optional life-stage restriction. When non-null, this warning is only shown
     * to check-ins submitted for this life stage.
     * Null means "applies to all life stages".
     */
    @Column(length = 80)
    private String requiredLifeStage;

    // ---- getters / setters ----

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public List<String> getTriggerSymptomSlugs() { return triggerSymptomSlugs; }
    public void setTriggerSymptomSlugs(List<String> triggerSymptomSlugs) { this.triggerSymptomSlugs = triggerSymptomSlugs; }
    public List<String> getRelatedConditionSlugs() { return relatedConditionSlugs; }
    public void setRelatedConditionSlugs(List<String> relatedConditionSlugs) { this.relatedConditionSlugs = relatedConditionSlugs; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceTitle() { return sourceTitle; }
    public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
    public int getMinMatches() { return minMatches; }
    public void setMinMatches(int minMatches) { this.minMatches = minMatches; }
    public String getRequiredLifeStage() { return requiredLifeStage; }
    public void setRequiredLifeStage(String requiredLifeStage) { this.requiredLifeStage = requiredLifeStage; }
}
