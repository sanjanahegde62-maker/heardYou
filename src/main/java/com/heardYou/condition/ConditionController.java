package com.heardYou.condition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

   
    @GetMapping
    public ResponseEntity<Page<ConditionSummaryDto>> list(
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String stage,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(conditionService.list(system, stage, pageable));
    }

    /** GET /api/conditions/{slug} */
    @GetMapping("/{slug}")
    public ResponseEntity<ConditionDetailDto> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(conditionService.getBySlug(slug));
    }
}
