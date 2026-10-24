package com.heardYou.symptom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SymptomRepository extends JpaRepository<Symptom, Long> {

    Optional<Symptom> findBySlug(String slug);

    @Query("SELECT DISTINCT s FROM Symptom s LEFT JOIN s.aliases a WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(a) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Symptom> searchByQuery(@Param("q") String q);

    List<Symptom> findBySlugIn(List<String> slugs);
}
