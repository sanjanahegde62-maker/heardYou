package com.heardYou.symptom;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * An observable symptom used in educational matching.
 */
@Entity
@Table(name = "symptoms",
        indexes = {
            @Index(name = "idx_symptom_slug", columnList = "slug", unique = true),
            @Index(name = "idx_symptom_name", columnList = "name")
        })
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** URL-safe identifier, e.g. "pelvic-pain". */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Broad grouping, e.g. "pain", "hormonal", "gastrointestinal". */
    @Column(length = 100)
    private String category;

    /**
     * Common alternative names or lay terms for this symptom.
     * Enables alias-based search (e.g. "cramps" → painful periods).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "symptom_aliases",
                     joinColumns = @JoinColumn(name = "symptom_id"))
    @Column(name = "alias", length = 200)
    private java.util.List<String> aliases = new java.util.ArrayList<>();

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    // ---- getters / setters ----

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public java.util.List<String> getAliases() { return aliases; }
    public void setAliases(java.util.List<String> aliases) { this.aliases = aliases; }
    public Instant getCreatedAt() { return createdAt; }
}
