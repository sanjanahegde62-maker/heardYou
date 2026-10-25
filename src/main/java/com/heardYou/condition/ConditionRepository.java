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

    @Query("SELECT c FROM Condition c ORDER BY CASE WHEN EXISTS (SELECT ls FROM Condition c2 JOIN c2.lifeStages ls WHERE c2.id = c.id AND ls = :lifeStage) THEN 0 ELSE 1 END, c.name")
    Page<Condition> findAllSortedByLifeStage(@Param("lifeStage") String lifeStage, Pageable pageable);

    @Query("SELECT c FROM Condition c WHERE c.category = :category ORDER BY CASE WHEN EXISTS (SELECT ls FROM Condition c2 JOIN c2.lifeStages ls WHERE c2.id = c.id AND ls = :lifeStage) THEN 0 ELSE 1 END, c.name")
    Page<Condition> findByCategoryAndSortByLifeStage(@Param("category") String category, @Param("lifeStage") String lifeStage, Pageable pageable);

    @Query("SELECT c FROM Condition c WHERE UPPER(c.bodySystem) = UPPER(:bodySystem)")
    Page<Condition> findByBodySystem(@Param("bodySystem") String bodySystem, Pageable pageable);

    @Query("SELECT c FROM Condition c WHERE UPPER(c.bodySystem) = UPPER(:bodySystem) ORDER BY CASE WHEN EXISTS (SELECT ls FROM Condition c2 JOIN c2.lifeStages ls WHERE c2.id = c.id AND ls = :lifeStage) THEN 0 ELSE 1 END, c.name")
    Page<Condition> findByBodySystemAndSortByLifeStage(@Param("bodySystem") String bodySystem, @Param("lifeStage") String lifeStage, Pageable pageable);

    /**
     * Full-text catalogue search across condition fields and linked symptom names/aliases.
     *
     * Searched fields:
     *   - condition name, summary, medicalDescription
     *   - conventionalPresentation, femalePresentation
     *   - names and descriptions of linked symptoms (via ConditionSymptom join)
     *   - aliases of linked symptoms
     *
     * This lets queries like "diarrhoea", "diarrhea", "stomach pain", "period pain",
     * "tired", and "headache" surface conditions that document those terms in their
     * linked symptom catalogue entries, even when the condition own text fields
     * do not contain the word.
     *
     * DISTINCT prevents duplicate rows when multiple linked symptoms match.
     */
    @Query("""
            SELECT DISTINCT c FROM Condition c
            WHERE LOWER(c.name)                     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.summary)                  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.medicalDescription)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.conventionalPresentation) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.femalePresentation)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR EXISTS (
                   SELECT 1 FROM ConditionSymptom cs
                   JOIN cs.symptom s
                   WHERE cs.condition = c
                     AND (LOWER(s.name)        LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(s.description) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR EXISTS (
                              SELECT 1 FROM Symptom s2 JOIN s2.aliases a
                              WHERE s2 = s
                                AND LOWER(a) LIKE LOWER(CONCAT('%', :q, '%'))
                          )
                     )
               )
            ORDER BY c.name
            """)
    List<Condition> searchByQuery(@Param("q") String q);
}
