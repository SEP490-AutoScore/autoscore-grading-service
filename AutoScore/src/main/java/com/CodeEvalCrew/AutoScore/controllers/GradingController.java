package com.CodeEvalCrew.AutoScore.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
import com.CodeEvalCrew.AutoScore.services.autoscore_postman_service.IAutoscorePostmanService;
import com.CodeEvalCrew.AutoScore.services.check_important.ICheckImportant;
import com.CodeEvalCrew.AutoScore.services.plagiarism_check_service.IPlagiarismDetectionService;
import com.CodeEvalCrew.AutoScore.services.score_service.IScoreService;


@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final ICheckImportant checkimportant;
    private final IAutoscorePostmanService autoscorePostmanService;
    private final IPlagiarismDetectionService plagiarismDetectionService;
    private final IScoreService scoreService;
    private final SSEController sseController;

    public GradingController(
            ICheckImportant checkimportant,
            IAutoscorePostmanService autoscorePostmanService,
            IPlagiarismDetectionService plagiarismDetectionService,
            SourceDetailRepository sourceDetailRepository,
            IScoreService scoreService,
            SSEController sseController) {
        this.autoscorePostmanService = autoscorePostmanService;
        this.checkimportant = checkimportant;
        this.plagiarismDetectionService = plagiarismDetectionService;
        this.scoreService = scoreService;
        this.sseController = sseController;
    }

    @PostMapping("")
    public ResponseEntity<?> grading(@RequestBody CheckImportantRequest request) {
        try {
            LocalDateTime s = LocalDateTime.now();
            
            System.out.println("--------- Check Important ---------");
            List<StudentSourceInfoDTO> listSourceInfoDTOs = checkimportant.checkImportantForGranding(request);
            System.out.println("--------- Grading ---------");
            List<StudentSourceInfoDTO> listStudentSourceInfoHaveScoreDTO = autoscorePostmanService.gradingFunction(listSourceInfoDTOs, request.getExamPaperId(), request.getNumberDeploy(),request.getMemory_Megabyte(), request.getProcessors());
            System.out.println("--------- Plagiarism Detection ---------");
            plagiarismDetectionService.runPlagiarismDetection(listStudentSourceInfoHaveScoreDTO, request.getExamType(), request.getOrganizationId(), request.getExamPaperId());
            System.out.println("--------- Add Student Error To Score ---------");
            scoreService.addStudentErrorToScore(request.getExamPaperId());
            System.out.println("--------- Done ---------");
            
            
            LocalDateTime tt = LocalDateTime.now();
            Duration duration = Duration.between(s, tt);
        System.out.println("Total time: " + duration.toMillis() + " milliseconds");

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            sseController.pushEvent(1l, "Error when processing because not found something", 0, 10, LocalDateTime.now());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            sseController.pushEvent(1l, "Error when processing", 0, 10, LocalDateTime.now());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("test")
    public String getMethodName() {
        sseController.pushEvent(1l, "Grading", 1, 10, LocalDateTime.now());
        return "new String()";
    }
    

}
