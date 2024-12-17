package com.CodeEvalCrew.AutoScore.services.grading_service;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;

public interface IGradingService {
    void gradingV2(CheckImportantRequest request);

    void grading(CheckImportantRequest request) throws Exception, NotFoundException;

    void updateGradingAt(Long examPaperId);
}
