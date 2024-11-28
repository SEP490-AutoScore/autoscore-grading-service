package com.CodeEvalCrew.AutoScore.services.grading_service;

import org.springframework.web.bind.annotation.RequestBody;

import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;

public interface IGradingService {
    void gradingV2(@RequestBody CheckImportantRequest request);
}
