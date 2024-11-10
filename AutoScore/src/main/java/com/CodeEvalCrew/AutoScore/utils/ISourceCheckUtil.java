package com.CodeEvalCrew.AutoScore.utils;

import java.util.List;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.Important.StudentSource;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.Important;

public interface ISourceCheckUtil {

    List<StudentSourceInfoDTO> getImportantToCheck(List<Important> importants, List<StudentSource> students, Exam_Paper examPaper) throws Exception, NotFoundException;
}
