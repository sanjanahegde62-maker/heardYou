package com.heardYou.condition;

import com.heardYou.source.MedicalSource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A health condition available in the educational catalogue.
 * No personal health data is stored here.
 */
@Entity
@Table(name = "conditions",
        indexes = {
            @Index(name = "idx_condition_slug", columnList = "slug", unique = true),
            @Index(name = "idx_condition_category", columnList = "category"),
            @Index(name = "idx_condition_body_system", columnList = "body_system")
        })
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** URL-safe identifier used in API paths, e.g. "endometriosis". */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String medicalDescription;

    /**
     * Body system grouping per the spec: REPRODUCTIVE, HORMONAL_METABOLIC,
     * CARDIOVASCULAR, BRAIN_MIND, AUTOIMMUNE, BONE_PAIN, OTHER.
     */
    @Column(name = "body_system", length = 60)
    private String bodySystem;

    /** How textbooks typically describe the presentation. */
    @Column(columnDefinition = "TEXT")
    private String conventionalPresentation;

    /**
     * How women actually experience this condition — the gap between textbook
     * and real-world presentation. Core to the "Textbook vs. Her" spec.
     */
    @Column(columnDefinition = "TEXT")
    private String femalePresentation;

    /** Documented real-world variations, e.g. atypical/delayed diagnosis patterns. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_documented_presentations",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "presentation", columnDefinition = "TEXT")
    private List<String> documentedPresentations = new ArrayList<>();

    /** Other conditions this is commonly confused with, to help the user articulate differences. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_mistaken_for",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "condition_name", length = 200)
    private List<String> commonlyMistakenFor = new ArrayList<>();

    /**
     * Average time to diagnosis where documented.
     * E.g. "7–10 years on average (WHO, 2023)".
     * Null if not reliably sourced.
     */
    @Column(length = 300)
    private String avgDiagnosisDelay;

    /** Tests typically suggested when this condition is being investigated. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_suggested_tests",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "test_name", length = 300)
    private List<String> suggestedTests = new ArrayList<>();

    /**
     * Timeframe guidance for when to seek help.
     * E.g. "If symptoms persist beyond 3 months, discuss with your GP."
     * Must not be phrased as a diagnosis.
     */
    @Column(columnDefinition = "TEXT")
    private String timeframeAdvice;

    /** High-level category tag, e.g. "reproductive", "autoimmune", "mental-health". */
    @Column(length = 100)
    private String category;

    /** Additional category tags beyond the primary one. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_extra_categories",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "category_tag", length = 100)
    private Set<String> extraCategories = new HashSet<>();

    /** Life stages where this condition is particularly relevant. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_life_stages",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "life_stage", length = 80)
    private Set<String> lifeStages = new HashSet<>();

    /** Type of specialist typically involved. */
    @Column(length = 200)
    private String specialistType;

    @Column(columnDefinition = "TEXT")
    private String specialistNotes;

    @Column(columnDefinition = "TEXT")
    private String educationalGuidance;

    /** Suggested questions a patient could bring to a clinician appointment. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_clinician_questions",
                     joinColumns = @JoinColumn(name = "condition_id"))
    @Column(name = "question", columnDefinition = "TEXT")
    private List<String> clinicianQuestions = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "condition_sources",
               joinColumns = @JoinColumn(name = "condition_id"),
               inverseJoinColumns = @JoinColumn(name = "source_id"))
    private Set<MedicalSource> sources = new HashSet<>();

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ---- getters / setters ----

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getMedicalDescription() { return medicalDescription; }
    public void setMedicalDescription(String v) { this.medicalDescription = v; }
    public String getBodySystem() { return bodySystem; }
    public void setBodySystem(String v) { this.bodySystem = v; }
    public String getConventionalPresentation() { return conventionalPresentation; }
    public void setConventionalPresentation(String v) { this.conventionalPresentation = v; }
    public String getFemalePresentation() { return femalePresentation; }
    public void setFemalePresentation(String v) { this.femalePresentation = v; }
    public List<String> getDocumentedPresentations() { return documentedPresentations; }
    public void setDocumentedPresentations(List<String> v) { this.documentedPresentations = v; }
    public List<String> getCommonlyMistakenFor() { return commonlyMistakenFor; }
    public void setCommonlyMistakenFor(List<String> v) { this.commonlyMistakenFor = v; }
    public String getAvgDiagnosisDelay() { return avgDiagnosisDelay; }
    public void setAvgDiagnosisDelay(String v) { this.avgDiagnosisDelay = v; }
    public List<String> getSuggestedTests() { return suggestedTests; }
    public void setSuggestedTests(List<String> v) { this.suggestedTests = v; }
    public String getTimeframeAdvice() { return timeframeAdvice; }
    public void setTimeframeAdvice(String v) { this.timeframeAdvice = v; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Set<String> getExtraCategories() { return extraCategories; }
    public void setExtraCategories(Set<String> v) { this.extraCategories = v; }
    public Set<String> getLifeStages() { return lifeStages; }
    public void setLifeStages(Set<String> v) { this.lifeStages = v; }
    public String getSpecialistType() { return specialistType; }
    public void setSpecialistType(String v) { this.specialistType = v; }
    public String getSpecialistNotes() { return specialistNotes; }
    public void setSpecialistNotes(String v) { this.specialistNotes = v; }
    public String getEducationalGuidance() { return educationalGuidance; }
    public void setEducationalGuidance(String v) { this.educationalGuidance = v; }
    public List<String> getClinicianQuestions() { return clinicianQuestions; }
    public void setClinicianQuestions(List<String> v) { this.clinicianQuestions = v; }
    public Set<MedicalSource> getSources() { return sources; }
    public void setSources(Set<MedicalSource> sources) { this.sources = sources; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
