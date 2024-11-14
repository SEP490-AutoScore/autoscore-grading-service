package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;

@Service
public class FinalCheckPlagiarismService {

    @Autowired
    private ScoreRepository scoreRepository;

    @SuppressWarnings("CallToPrintStackTrace")
    void finalCheckPlagiarism(Long examPaperId) {
        try {
            List<Score> scores = scoreRepository.findByExamPaperExamPaperId(examPaperId);
            for (Score score : scores) {
                if (score.getLevelOfPlagiarism() == null) {
                    score.setLevelOfPlagiarism("NO CHECK PLAGIARISM");
                    score.setPlagiarismReason("N/A");
                    scoreRepository.save(score);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
