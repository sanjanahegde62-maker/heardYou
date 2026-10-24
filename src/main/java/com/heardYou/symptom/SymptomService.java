package com.heardYou.symptom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SymptomService {

    private final SymptomRepository symptomRepository;

    public SymptomService(SymptomRepository symptomRepository) {
        this.symptomRepository = symptomRepository;
    }

    public List<SymptomDetailDto> list() {
        return symptomRepository.findAll().stream()
                .map(SymptomDetailDto::from)
                .toList();
    }

    public List<SymptomDetailDto> search(String query) {
        return symptomRepository.searchByQuery(query).stream()
                .map(SymptomDetailDto::from)
                .toList();
    }
}
