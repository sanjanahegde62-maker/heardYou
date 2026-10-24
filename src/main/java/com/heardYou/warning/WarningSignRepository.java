package com.heardYou.warning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarningSignRepository extends JpaRepository<WarningSign, Long> {

    /** Returns all warning signs that have at least one trigger slug matching the given list. */
    @Query("SELECT DISTINCT w FROM WarningSign w JOIN w.triggerSymptomSlugs s WHERE s IN :slugs")
    List<WarningSign> findByTriggerSymptomSlugIn(@Param("slugs") List<String> slugs);

    /** Used for idempotent seeding — check by name before inserting. */
    Optional<WarningSign> findByName(String name);
}
