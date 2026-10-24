package com.heardYou.condition;

import com.heardYou.symptom.Symptom;
import jakarta.persistence.*;

/**
 * Editorial mapping between a Condition and a Symptom.
 * {@code overlapWeight} is an editorial score (0.0–1.0) encoding how strongly
 * this symptom is documented for the condition.  It is NOT a clinical
 * probability and must never be presented as such.
 */
@Entity
@Table(name = "condition_symptoms",
        indexes = {
            @Index(name = "idx_cs_condition", columnList = "condition_id"),
            @Index(name = "idx_cs_symptom",   columnList = "symptom_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_condition_symptom",
                columnNames = {"condition_id", "symptom_id"}))
public class ConditionSymptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_id", nullable = false)
    private Condition condition;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;

    /**
     * Editorial overlap weight: 0.0 = rarely documented, 1.0 = very strongly documented.
     * This is assigned by editorial review, not computed from patient data.
     */
    @Column(nullable = false)
    private double overlapWeight = 0.5;

    /** Free-text note about the evidence basis for this mapping. */
    @Column(columnDefinition = "TEXT")
    private String evidenceNote;

    // ---- getters / setters ----

    public Long getId() { return id; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public Symptom getSymptom() { return symptom; }
    public void setSymptom(Symptom symptom) { this.symptom = symptom; }
    public double getOverlapWeight() { return overlapWeight; }
    public void setOverlapWeight(double overlapWeight) { this.overlapWeight = overlapWeight; }
    public String getEvidenceNote() { return evidenceNote; }
    public void setEvidenceNote(String evidenceNote) { this.evidenceNote = evidenceNote; }
}
