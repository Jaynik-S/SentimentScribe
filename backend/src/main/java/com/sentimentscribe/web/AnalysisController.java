package com.sentimentscribe.web;

import com.sentimentscribe.service.AnalysisService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.usecase.analyze_keywords.AnalyzeKeywordsOutputData;
import com.sentimentscribe.web.dto.AnalysisRequest;
import com.sentimentscribe.web.dto.AnalysisResponse;
import com.sentimentscribe.web.dto.ErrorResponse;
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
