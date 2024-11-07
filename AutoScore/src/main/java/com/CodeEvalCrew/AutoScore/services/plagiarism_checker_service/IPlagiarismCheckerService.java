package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

import java.util.List;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;

public interface IPlagiarismCheckerService {
    PlagiarismResult checkPlagiarism(List<StudentSourceInfoDTO> studentSourceToCheck);
}
