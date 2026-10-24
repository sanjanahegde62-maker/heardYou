package com.heardYou.brief;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brief")
public class BriefController {

    private final BriefService briefService;

    public BriefController(BriefService briefService) {
        this.briefService = briefService;
    }

    /** POST /api/brief — returns a downloadable PDF. Nothing is persisted. */
    @PostMapping(produces = "application/pdf")
    public ResponseEntity<byte[]> generateBrief(@Valid @RequestBody BriefRequest request) {
        byte[] pdf = briefService.generatePdf(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"appointment-brief.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
