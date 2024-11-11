package com.CodeEvalCrew.AutoScore.services.autoscore_postman_service;

import java.util.List;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoHaveScoreDTO;

public interface IAutoscorePostmanService {
    List<StudentSourceInfoHaveScoreDTO> gradingFunction(List<StudentSourceInfoDTO> studentSources, Long examPaperId, int numberDeploy);

}
