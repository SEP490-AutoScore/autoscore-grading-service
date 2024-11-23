package com.CodeEvalCrew.AutoScore.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
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

    private final SourceDetailRepository sourceDetailRepository;

    public GradingController(
            ICheckImportant checkimportant,
            IAutoscorePostmanService autoscorePostmanService,
            IPlagiarismDetectionService plagiarismDetectionService,
            SourceDetailRepository sourceDetailRepository,
            IScoreService scoreService) {
        this.autoscorePostmanService = autoscorePostmanService;
        this.checkimportant = checkimportant;
        this.plagiarismDetectionService = plagiarismDetectionService;
        this.scoreService = scoreService;
        this.sourceDetailRepository = sourceDetailRepository;
    }

    @PostMapping("")
    public ResponseEntity<?> grading(@RequestBody CheckImportantRequest request) {
        try {

            List<Source_Detail> list = sourceDetailRepository.findAll();
            List<Long> ax = new ArrayList<>();

            for (Source_Detail id : list) {
                ax.add(id.getStudent().getStudentId());
            }
            request.setListStudent(ax);

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
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
