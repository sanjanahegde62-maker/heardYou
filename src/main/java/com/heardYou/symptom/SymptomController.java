package com.heardYou.symptom;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/symptoms")
public class SymptomController {

    private final SymptomService symptomService;

    public SymptomController(SymptomService symptomService) {
        this.symptomService = symptomService;
    }

    /** GET /api/symptoms?q= */
    @GetMapping
    public ResponseEntity<List<SymptomDetailDto>> list(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(symptomService.search(q));
        }
        return ResponseEntity.ok(symptomService.list());
    }
}
