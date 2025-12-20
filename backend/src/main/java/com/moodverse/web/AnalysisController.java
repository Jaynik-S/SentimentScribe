package com.moodverse.web;

import com.moodverse.service.AnalysisService;
import com.moodverse.service.ServiceResult;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsOutputData;
import com.moodverse.web.dto.AnalysisRequest;
import com.moodverse.web.dto.AnalysisResponse;
import com.moodverse.web.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody AnalysisRequest request) {
        ServiceResult<AnalyzeKeywordsOutputData> result = analysisService.analyze(request.text());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        return ResponseEntity.ok(new AnalysisResponse(result.data().getKeywords()));
    }
}
