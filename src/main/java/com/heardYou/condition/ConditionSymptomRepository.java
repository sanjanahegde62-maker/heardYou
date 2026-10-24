package com.heardYou.condition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConditionSymptomRepository extends JpaRepository<ConditionSymptom, Long> {

    List<ConditionSymptom> findByCondition(Condition condition);

    @Query("SELECT cs FROM ConditionSymptom cs WHERE cs.condition = :condition ORDER BY cs.overlapWeight DESC")
    List<ConditionSymptom> findByConditionOrderByWeight(@Param("condition") Condition condition);

    /** Returns all mappings for the given symptom slugs – used by the matching engine. */
    @Query("SELECT cs FROM ConditionSymptom cs WHERE cs.symptom.slug IN :slugs")
    List<ConditionSymptom> findBySymptomSlugIn(@Param("slugs") List<String> slugs);
}
