package com.heardYou.condition;

import com.heardYou.source.SourceDto;
import com.heardYou.symptom.SymptomSummaryDto;

import java.util.List;
import java.util.Set;


public record ConditionDetailDto(
        Long id,
        String name,
        String slug,
        String summary,
        String medicalDescription,
        String bodySystem,
        String conventionalPresentation,
        String femalePresentation,
        List<String> documentedPresentations,
        List<String> commonlyMistakenFor,
        String avgDiagnosisDelay,
        List<String> suggestedTests,
        String timeframeAdvice,
        String category,
        Set<String> extraCategories,
        Set<String> lifeStages,
        String specialistType,
        String specialistNotes,
        String educationalGuidance,
        List<String> clinicianQuestions,
        List<ConditionSymptomDto> symptoms,
        List<SourceDto> sources
) {
    public record ConditionSymptomDto(
            SymptomSummaryDto symptom,
            double overlapWeight,
            String evidenceNote
    ) {}

    public static ConditionDetailDto from(Condition c, List<ConditionSymptom> mappings) {
        var symptomDtos = mappings.stream()
                .map(cs -> new ConditionSymptomDto(
                        SymptomSummaryDto.from(cs.getSymptom()),
                        cs.getOverlapWeight(),
                        cs.getEvidenceNote()))
                .toList();
        var sourceDtos = c.getSources().stream()
                .map(SourceDto::from)
                .toList();
        return new ConditionDetailDto(
                c.getId(), c.getName(), c.getSlug(), c.getSummary(),
                c.getMedicalDescription(), c.getBodySystem(),
                c.getConventionalPresentation(), c.getFemalePresentation(),
                c.getDocumentedPresentations(), c.getCommonlyMistakenFor(),
                c.getAvgDiagnosisDelay(), c.getSuggestedTests(), c.getTimeframeAdvice(),
                c.getCategory(), c.getExtraCategories(),
                c.getLifeStages(), c.getSpecialistType(), c.getSpecialistNotes(),
                c.getEducationalGuidance(), c.getClinicianQuestions(),
                symptomDtos, sourceDtos);
    }
}
