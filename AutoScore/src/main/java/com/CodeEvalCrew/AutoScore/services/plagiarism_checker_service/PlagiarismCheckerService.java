package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;

@Service
public class PlagiarismCheckerService implements IPlagiarismCheckerService {
    private final SourceDetailRepository sourceDetailRepository;

    public PlagiarismCheckerService (SourceDetailRepository sourceDetailRepository){
        this.sourceDetailRepository = sourceDetailRepository;
    }

    @Override
    public PlagiarismResult checkPlagiarism(List<StudentSourceInfoDTO> studentSourceToCheck) {
        try {
            PlagiarismResult report = new PlagiarismResult();
            // Lấy danh sách source detail
            List<Source_Detail> sourceDetails = new ArrayList<>();
            for (StudentSourceInfoDTO studentSource : studentSourceToCheck) {
                Optional<Source_Detail> sourceDetail = sourceDetailRepository.findById(studentSource.getSourceDetailId());
                if (sourceDetail.isEmpty()){
                    sourceDetails.add(sourceDetail.get());
                }
            }
            // Lấy danh sách source detail tại campus

            for (Source_Detail studentSrc : sourceDetails) {
                for (Source_Detail dbStudentSrc : sourceDetails) {
                    if (!Objects.equals(studentSrc.getSourceDetailId(), dbStudentSrc.getSourceDetailId())) {
                        // report.add(basicCheck.run(student, dbStudent));
                        // report.add(intermediateCheck.run(student, dbStudent));
                        // report.add(advancedCheck.run(student, dbStudent));
                    }
                }
            }
            return report;
        } catch (Exception e) {
           return null;
        }
    }

}
