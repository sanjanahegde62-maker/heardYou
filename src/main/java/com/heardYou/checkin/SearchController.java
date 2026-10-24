package com.heardYou.checkin;

import com.heardYou.condition.ConditionService;
import com.heardYou.symptom.SymptomDetailDto;
import com.heardYou.symptom.SymptomService;
import com.heardYou.condition.ConditionSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ConditionService conditionService;
    private final SymptomService symptomService;

    public SearchController(ConditionService conditionService, SymptomService symptomService) {
        this.conditionService = conditionService;
        this.symptomService = symptomService;
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false, defaultValue = "") String q) {
     
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(Map.of(
                    "conditions", List.of(),
                    "symptoms", List.of(),
                    "query", q == null ? "" : q));
        }
        String trimmed = q.trim();
        List<ConditionSummaryDto> conditions = conditionService.search(trimmed);
        List<SymptomDetailDto> symptoms = symptomService.search(trimmed);
        return ResponseEntity.ok(Map.of(
                "conditions", conditions,
                "symptoms", symptoms,
                "query", trimmed));
    }
}
