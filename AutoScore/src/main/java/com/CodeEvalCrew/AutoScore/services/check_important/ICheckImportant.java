package com.CodeEvalCrew.AutoScore.services.check_important;

import java.util.List;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;

public interface  ICheckImportant {
    List<StudentSourceInfoDTO> checkImportantForGranding(CheckImportantRequest request) throws Exception, NotFoundException;
}
