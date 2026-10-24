package com.heardYou.source;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A citable medical or educational source (journal, guideline, organisation).
 */
@Entity
@Table(name = "medical_sources")
public class MedicalSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    /** Publisher, journal, or authoritative organisation. */
    @Column(length = 300)
    private String publisher;

    @Column(length = 1000)
    private String url;

    /** Date the source was published or last reviewed, if known. */
    private LocalDate reviewDate;

    /** Short descriptions of the specific claims this source supports. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "medical_source_claims",
                     joinColumns = @JoinColumn(name = "source_id"))
    @Column(name = "claim", columnDefinition = "TEXT")
    private List<String> supportedClaims = new ArrayList<>();

    /**
     * True when editorial review of the source URL / claims has not been completed.
     * Claims from sources with this flag must be presented with an explicit caveat.
     */
    @Column(nullable = false)
    private boolean needsEditorialReview = false;

    // ---- getters / setters ----

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }
    public List<String> getSupportedClaims() { return supportedClaims; }
    public void setSupportedClaims(List<String> supportedClaims) { this.supportedClaims = supportedClaims; }
    public boolean isNeedsEditorialReview() { return needsEditorialReview; }
    public void setNeedsEditorialReview(boolean needsEditorialReview) { this.needsEditorialReview = needsEditorialReview; }
}
