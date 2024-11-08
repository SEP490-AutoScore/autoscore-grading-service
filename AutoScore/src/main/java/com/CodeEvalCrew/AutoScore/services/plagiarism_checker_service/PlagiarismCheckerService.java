// package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;
// import java.util.Optional;

// import org.springframework.stereotype.Service;

// import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
// import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;
// import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
// import com.CodeEvalCrew.AutoScore.repositories.plagiarism_repository.PlagiarismResultRepository;
// import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;

// @Service
// public class PlagiarismCheckerService implements IPlagiarismCheckerService {
//     private final SourceDetailRepository sourceDetailRepository;
//     private final PlagiarismResultRepository plagiarismResultRepository;
//     private final BasicCheck basicCheck;
//     private final IntermediateCheck intermediateCheck;
//     private final AdvancedCheck advancedCheck;

//     public PlagiarismCheckerService (SourceDetailRepository sourceDetailRepository, BasicCheck basicCheck, 
//     IntermediateCheck intermediateCheck, AdvancedCheck advancedCheck, PlagiarismResultRepository plagiarismResultRepository) {
//         this.sourceDetailRepository = sourceDetailRepository;
//         this.basicCheck = basicCheck;
//         this.intermediateCheck = intermediateCheck;
//         this.advancedCheck = advancedCheck;
//         this.plagiarismResultRepository = plagiarismResultRepository;
//     }

//     @Override
//     public String checkPlagiarism(List<StudentSourceInfoDTO> studentSourceToCheck, String examType, Long organizationId) {
//         try {
//             List<PlagiarismResult> reports = new ArrayList<>();
//             // Lấy danh sách source detail
//             List<Source_Detail> sourceDetails = new ArrayList<>();
//             for (StudentSourceInfoDTO studentSource : studentSourceToCheck) {
//                 Optional<Source_Detail> sourceDetail = sourceDetailRepository.findById(studentSource.getSourceDetailId());
//                 if (sourceDetail.isEmpty()){
//                     sourceDetails.add(sourceDetail.get());
//                 }
//             }
//             // Lấy tất cả source detail dựa vào campus và exam type
//             List<Source_Detail> dbSourceDetails = sourceDetailRepository.findAllByTypeAndStudentOrganizationOrganizationId(examType, organizationId);

//             for (Source_Detail sourceDetail : sourceDetails) {
//                 for (Source_Detail dbSourceDetail : dbSourceDetails) {
//                     if (!Objects.equals(sourceDetail.getSourceDetailId(), dbSourceDetail.getSourceDetailId())) {
//                         reports.add(basicCheck.run(sourceDetail, dbSourceDetail));
//                         reports.add(intermediateCheck.run(sourceDetail, dbSourceDetail));
//                         reports.add(advancedCheck.run(sourceDetail, dbSourceDetail));
//                     }
//                 }
//             }

//             return savePlagiarismResult(reports);
//         } catch (Exception e) {
//            return "Check plagiarism error: " + e.getMessage();
//         }
//     }

//     private String savePlagiarismResult(List<PlagiarismResult> reports) {
//         try {
//             List<PlagiarismResult> savedReports = plagiarismResultRepository.saveAll(reports);
//             if (savedReports.size() == reports.size()) {
//                 return "Success for " + savedReports.size() + " source details.";
//             }
//         } catch (Exception e) {
//             return "Save plagiarism result error: " + e.getMessage();
//         }
//         return "Fail for " + reports.size() + " source details.";
//     }

// }
