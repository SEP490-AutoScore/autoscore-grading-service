package com.CodeEvalCrew.AutoScore.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.services.plagiarism_check_service.IPlagiarismDetectionService;

@RestController
@RequestMapping("/plagiarismCheck")
public class PlagiarismCheckController {

    @Autowired
    private IPlagiarismDetectionService plagiarismDetectionService;

    @PostMapping("/runPlagiarismDetection")
    public void runPlagiarismDetection(@RequestBody List<StudentSourceInfoDTO> sourceDetails) {
        try {
            plagiarismDetectionService.runPlagiarismDetection(sourceDetails);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
