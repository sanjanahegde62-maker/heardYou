package com.heardYou.checkin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinMatchingService checkinMatchingService;

    public CheckinController(CheckinMatchingService checkinMatchingService) {
        this.checkinMatchingService = checkinMatchingService;
    }

    /** POST /api/checkin */
    @PostMapping
    public ResponseEntity<CheckinResponse> checkin(@Valid @RequestBody CheckinRequest request) {
        return ResponseEntity.ok(checkinMatchingService.match(request));
    }
}
