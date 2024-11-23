package com.CodeEvalCrew.AutoScore.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.services.score_service.IScoreService;

@RestController
@RequestMapping("/api/score")
public class ScoreController {
    @Autowired
    private IScoreService scoreService;

    @PostMapping("/addStudentErrorToScore")
    public void addStudentErrorToScore(Long examPaperId) {
        scoreService.addStudentErrorToScore(examPaperId);
    }
}