package com.heardYou.condition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConditionRepository extends JpaRepository<Condition, Long> {

    Optional<Condition> findBySlug(String slug);

    List<Condition> findBySlugIn(List<String> slugs);

    Page<Condition> findByCategory(String category, Pageable pageable);

    @Query("""
           SELECT DISTINCT c FROM Condition c
           ORDER BY CASE WHEN :lifeStage MEMBER OF c.lifeStages THEN 0 ELSE 1 END, c.name
           """)
    Page<Condition> findAllSortedByLifeStage(@Param("lifeStage") String lifeStage, Pageable pageable);

    /**
     * Filters by category and sorts life-stage-tagged conditions first.
     */
    @Query("""
           SELECT DISTINCT c FROM Condition c
           WHERE c.category = :category
           ORDER BY CASE WHEN :lifeStage MEMBER OF c.lifeStages THEN 0 ELSE 1 END, c.name
           """)
    Page<Condition> findByCategoryAndSortByLifeStage(@Param("category") String category,
                                                      @Param("lifeStage") String lifeStage,
                                                      Pageable pageable);

    /** Filter by bodySystem (e.g. REPRODUCTIVE, CARDIOVASCULAR) — case-insensitive. */
    @Query("SELECT DISTINCT c FROM Condition c WHERE UPPER(c.bodySystem) = UPPER(:bodySystem)")
    Page<Condition> findByBodySystem(@Param("bodySystem") String bodySystem, Pageable pageable);

    /** Filter by bodySystem + sort life-stage-tagged conditions first. */
    @Query("""
           SELECT DISTINCT c FROM Condition c
           WHERE UPPER(c.bodySystem) = UPPER(:bodySystem)
           ORDER BY CASE WHEN :lifeStage MEMBER OF c.lifeStages THEN 0 ELSE 1 END, c.name
           """)
    Page<Condition> findByBodySystemAndSortByLifeStage(@Param("bodySystem") String bodySystem,
                                                        @Param("lifeStage") String lifeStage,
                                                        Pageable pageable);

    @Query("SELECT DISTINCT c FROM Condition c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.summary) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Condition> searchByQuery(@Param("q") String q);
}
