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

    public Page<ConditionSummaryDto> list(String category, String lifeStage, Pageable pageable) {
        Page<Condition> page;
        if (category != null && lifeStage != null) {
            
            page = conditionRepository.findByCategoryAndSortByLifeStage(category, lifeStage, pageable);
        } else if (category != null) {
            page = conditionRepository.findByCategory(category, pageable);
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
