package com.CodeEvalCrew.AutoScore.services.grading_service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Exam_Status_Enum;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Notification_Type_Enum;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.models.Entity.Notification;
import com.CodeEvalCrew.AutoScore.repositories.exam_repository.IExamPaperRepository;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;
import com.CodeEvalCrew.AutoScore.repositories.notification_repository.NotificationRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
import com.CodeEvalCrew.AutoScore.services.autoscore_postman_service.IAutoscorePostmanService;
import com.CodeEvalCrew.AutoScore.services.check_important.ICheckImportant;
import com.CodeEvalCrew.AutoScore.services.plagiarism_check_service.IPlagiarismDetectionService;
import com.CodeEvalCrew.AutoScore.services.score_service.IScoreService;
import com.CodeEvalCrew.AutoScore.utils.SendNotificationUtil;

@Service
public class GradingService implements IGradingService {

    private final ICheckImportant checkimportant;
    private final IAutoscorePostmanService autoscorePostmanService;
    private final IPlagiarismDetectionService plagiarismDetectionService;
    private final IScoreService scoreService;
    private final SSEController sseController;
    private final GradingProcessRepository gradingProcessRepository;
    private final IExamPaperRepository examPaperRepository;
    private final NotificationRepository notiRepo;
    private final SendNotificationUtil sendNotificationUtil;

    public GradingService(
            ICheckImportant checkimportant,
            IAutoscorePostmanService autoscorePostmanService,
            IPlagiarismDetectionService plagiarismDetectionService,
            SendNotificationUtil sendNotificationUtil,
            SourceDetailRepository sourceDetailRepository,
            IScoreService scoreService,
            NotificationRepository notiRepo,
            IExamPaperRepository examPaperRepository,
            GradingProcessRepository gradingProcessRepository,
            SSEController sseController) {
        this.autoscorePostmanService = autoscorePostmanService;
        this.checkimportant = checkimportant;
        this.plagiarismDetectionService = plagiarismDetectionService;
        this.scoreService = scoreService;
        this.sseController = sseController;
        this.examPaperRepository = examPaperRepository;
        this.gradingProcessRepository = gradingProcessRepository;
        this.notiRepo = notiRepo;
        this.sendNotificationUtil = sendNotificationUtil;
    }

    @Override
    public void gradingV2(CheckImportantRequest request) {
        boolean flag;
        do {
            GradingProcess gradingProcess = gradingProcessRepository
                    .findByExamPaper_ExamPaperId(request.getExamPaperId()).get();
            // if (gradingProcess == null) {
            // throw new Exception("ExamPaper not found!");
            // }
            Exam_Paper examPaper = examPaperRepository.findById(request.getExamPaperId()).get();
            try {
                System.out.println("--------- Check Important ---------");
                List<StudentSourceInfoDTO> listSourceInfoDTOs = checkimportant.checkImportantForGranding(request);
                System.out.println("--------- Grading ---------");
                // Print out the number of StudentSourceInfoDTO
                System.out.println("Number of StudentSourceInfoDTOs before grading: " + listSourceInfoDTOs.size());
                List<StudentSourceInfoDTO> listStudentSourceInfoHaveScoreDTO = autoscorePostmanService
                        .gradingFunction(listSourceInfoDTOs, request.getExamPaperId());
                System.out.println("--------- Plagiarism Detection ---------");
                plagiarismDetectionService.runPlagiarismDetection(listStudentSourceInfoHaveScoreDTO,
                        request.getExamType(), request.getOrganizationId(), request.getExamPaperId());
                System.out.println("--------- Add Student Error To Score ---------");
                scoreService.addStudentErrorToScore(request.getExamPaperId());
                System.out.println("--------- Done ---------");
                // set grading process
                gradingProcess.setStatus(GradingStatusEnum.DONE);
                gradingProcessRepository.save(gradingProcess);
                sseController.pushGradingProcess(gradingProcess.getProcessId(), GradingStatusEnum.DONE,
                        LocalDateTime.now(), gradingProcess.getExamPaper().getExamPaperId());
                // set exam paper status
                examPaper.setStatus(Exam_Status_Enum.COMPLETE);
                examPaperRepository.save(examPaper);
                // Noti
                Notification noti = new Notification(null, "Grading",
                        "Grading exam paper " + examPaper.getExamPaperCode() + " Done!", "/exams",
                        Notification_Type_Enum.SUCCESS, null);
                noti = notiRepo.save(noti);
                sendNotificationUtil.sendNotification(noti, gradingProcess.getCreateBy());

            } catch (NotFoundException | Exception e) {
                System.out.println(e.getCause());
                gradingProcess.setStatus(GradingStatusEnum.ERROR);
                gradingProcessRepository.save(gradingProcess);
                sseController.pushGradingProcess(gradingProcess.getProcessId(), GradingStatusEnum.ERROR,
                        LocalDateTime.now(), gradingProcess.getExamPaper().getExamPaperId());
                // set exam paper status
                examPaper.setStatus(Exam_Status_Enum.COMPLETE);
                examPaperRepository.save(examPaper);
                // Noti
                Notification noti = new Notification(null, "Grading",
                        "Grading exam paper " + examPaper.getExamPaperCode() + " Error!", "/exams",
                        Notification_Type_Enum.ERROR, null);
                noti = notiRepo.save(noti);
                sendNotificationUtil.sendNotification(noti, gradingProcess.getCreateBy());

            }
            gradingProcess = gradingProcessRepository.findFirstByStatus(GradingStatusEnum.PENDING);
            if (gradingProcess != null) {
                flag = true;
                request.setExamPaperId(gradingProcess.getExamPaper().getExamPaperId());
                request.setExamType(gradingProcess.getExamType().toString());
                request.setOrganizationId(gradingProcess.getOrganizationId());
                request.setListStudent(gradingProcess.getStudentIds());
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
            List<StudentSourceInfoDTO> listStudentSourceInfoHaveScoreDTO = autoscorePostmanService
                    .gradingFunction(listSourceInfoDTOs, request.getExamPaperId());
            System.out.println("--------- Plagiarism Detection ---------");
            plagiarismDetectionService.runPlagiarismDetection(listStudentSourceInfoHaveScoreDTO, request.getExamType(),
                    request.getOrganizationId(), request.getExamPaperId());
            System.out.println("--------- Add Student Error To Score ---------");
            scoreService.addStudentErrorToScore(request.getExamPaperId());
            System.out.println("--------- Done ---------");
        } catch (Exception | NotFoundException e) {
            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(),
                    request.getExamPaperId());
        }
    }
   
}
