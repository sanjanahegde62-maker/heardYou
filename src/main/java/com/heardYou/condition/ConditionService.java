package com.heardYou.condition;

import com.heardYou.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final ConditionSymptomRepository conditionSymptomRepository;

    public ConditionService(ConditionRepository conditionRepository,
                             ConditionSymptomRepository conditionSymptomRepository) {
        this.conditionRepository = conditionRepository;
        this.conditionSymptomRepository = conditionSymptomRepository;
    }

    /**
     * Lists conditions with optional filtering by bodySystem and/or lifeStage.
     *
     * @param bodySystem  e.g. "REPRODUCTIVE", "CARDIOVASCULAR" — filters on Condition.bodySystem
     * @param lifeStage   e.g. "reproductive-age" — when provided, life-stage-tagged conditions rank first
     * @param pageable    pagination and sorting
     */
    public Page<ConditionSummaryDto> list(String bodySystem, String lifeStage, Pageable pageable) {
        Page<Condition> page;
        if (bodySystem != null && lifeStage != null) {
            page = conditionRepository.findByBodySystemAndSortByLifeStage(bodySystem, lifeStage, pageable);
        } else if (bodySystem != null) {
            page = conditionRepository.findByBodySystem(bodySystem, pageable);
        } else if (lifeStage != null) {
            page = conditionRepository.findAllSortedByLifeStage(lifeStage, pageable);
        } else {
            page = conditionRepository.findAll(pageable);
        }
        return page.map(ConditionSummaryDto::from);
    }

    public ConditionDetailDto getBySlug(String slug) {
        Condition c = conditionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found: " + slug));
        List<ConditionSymptom> mappings = conditionSymptomRepository.findByConditionOrderByWeight(c);
        return ConditionDetailDto.from(c, mappings);
    }

    public List<ConditionSummaryDto> search(String query) {
        return conditionRepository.searchByQuery(query).stream()
                .map(ConditionSummaryDto::from)
                .toList();
    }
}
