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

    /** GET /api/symptoms?q= — requires at least 2 characters to search; shorter queries return empty */
    @GetMapping
    public ResponseEntity<List<SymptomDetailDto>> list(@RequestParam(required = false) String q) {
        if (q != null && q.trim().length() >= 2) {
            return ResponseEntity.ok(symptomService.search(q));
        }
        if (q != null && !q.isBlank()) {
            // 1-character query — return empty per spec (no error)
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(symptomService.list());
    }
}
