package com.CodeEvalCrew.AutoScore.services.grading_service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;
import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
import com.CodeEvalCrew.AutoScore.services.autoscore_postman_service.IAutoscorePostmanService;
import com.CodeEvalCrew.AutoScore.services.check_important.ICheckImportant;
import com.CodeEvalCrew.AutoScore.services.plagiarism_check_service.IPlagiarismDetectionService;
import com.CodeEvalCrew.AutoScore.services.score_service.IScoreService;

@Service
public class GradingService implements IGradingService {

    private final ICheckImportant checkimportant;
    private final IAutoscorePostmanService autoscorePostmanService;
    private final IPlagiarismDetectionService plagiarismDetectionService;
    private final IScoreService scoreService;
    private final SSEController sseController;
    private final GradingProcessRepository gradingProcessRepository;

    public GradingService(
            ICheckImportant checkimportant,
            IAutoscorePostmanService autoscorePostmanService,
            IPlagiarismDetectionService plagiarismDetectionService,
            SourceDetailRepository sourceDetailRepository,
            IScoreService scoreService,
            GradingProcessRepository gradingProcessRepository,
            SSEController sseController) {
        this.autoscorePostmanService = autoscorePostmanService;
        this.checkimportant = checkimportant;
        this.plagiarismDetectionService = plagiarismDetectionService;
        this.scoreService = scoreService;
        this.sseController = sseController;
        this.gradingProcessRepository = gradingProcessRepository;
    }

    @Override
    public void gradingV2(CheckImportantRequest request) {
        boolean flag;
        do {
            GradingProcess gradingProcess = gradingProcessRepository.findByExamPaper_ExamPaperId(request.getExamPaperId()).get();
            // if (gradingProcess == null) {
            //     throw new Exception("ExamPaper not found!");
            // }
            try {
                System.out.println("--------- Check Important ---------");
                List<StudentSourceInfoDTO> listSourceInfoDTOs = checkimportant.checkImportantForGranding(request);
                System.out.println("--------- Grading ---------");
                List<StudentSourceInfoDTO> listStudentSourceInfoHaveScoreDTO = autoscorePostmanService.gradingFunction(listSourceInfoDTOs, request.getExamPaperId());
                System.out.println("--------- Plagiarism Detection ---------");
                plagiarismDetectionService.runPlagiarismDetection(listStudentSourceInfoHaveScoreDTO, request.getExamType(), request.getOrganizationId(), request.getExamPaperId());
                System.out.println("--------- Add Student Error To Score ---------");
                scoreService.addStudentErrorToScore(request.getExamPaperId());
                System.out.println("--------- Done ---------");
            } catch (NotFoundException | Exception e) {
                System.out.println(e.getCause());
                gradingProcess.setStatus(GradingStatusEnum.ERROR);
                gradingProcessRepository.save(gradingProcess);
                sseController.pushGradingProcess(gradingProcess.getProcessId(), GradingStatusEnum.ERROR, LocalDateTime.now(), gradingProcess.getExamPaper().getExamPaperId());
            }

            GradingProcess nextGradingProcess = gradingProcessRepository.findFirstByStatus(GradingStatusEnum.PENDING);
            if (nextGradingProcess != null) {
                flag = true;
                request.setExamPaperId(nextGradingProcess.getExamPaper().getExamPaperId());
                request.setExamType(nextGradingProcess.getExamType().toString());
                request.setOrganizationId(nextGradingProcess.getOrganizationId());
                request.setListStudent(nextGradingProcess.getStudentIds());
            } else {
                flag = false;
            }
        } while (flag);
    }

    @Override
    public void grading(CheckImportantRequest request) throws Exception, NotFoundException {
        try {
            System.out.println("--------- Check Important ---------");
            List<StudentSourceInfoDTO> listSourceInfoDTOs = checkimportant.checkImportantForGranding(request);
            System.out.println("--------- Grading ---------");
            List<StudentSourceInfoDTO> listStudentSourceInfoHaveScoreDTO = autoscorePostmanService.gradingFunction(listSourceInfoDTOs, request.getExamPaperId());
            System.out.println("--------- Plagiarism Detection ---------");
            plagiarismDetectionService.runPlagiarismDetection(listStudentSourceInfoHaveScoreDTO, request.getExamType(), request.getOrganizationId(), request.getExamPaperId());
            System.out.println("--------- Add Student Error To Score ---------");
            scoreService.addStudentErrorToScore(request.getExamPaperId());
            System.out.println("--------- Done ---------");
        } catch (Exception | NotFoundException e) {
            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), request.getExamPaperId());
        }
    }

}
