package com.CodeEvalCrew.AutoScore.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.services.plagiarism_check_service.IPlagiarismDetectionService;

@RestController
@RequestMapping("/plagiarismCheck")
public class PlagiarismCheckController {

    @Autowired
    private IPlagiarismDetectionService plagiarismDetectionService;

    @PostMapping("/runPlagiarismDetection")
    public void runPlagiarismDetection(@RequestBody List<StudentSourceInfoDTO> sourceDetails, 
    @RequestParam("exam_type") String examType, @RequestParam("organization_id") Long organizationId, @RequestParam("exam_paper_id") Long examPaperId) {
        try {
            plagiarismDetectionService.runPlagiarismDetection(sourceDetails, examType, organizationId, examPaperId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
