package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;

public interface IPlagiarismDetectionService {
    void runPlagiarismDetection(List<StudentSourceInfoDTO> sourceDetails, String examType, Long organizationId);
}
