package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.Entity.Code_Plagiarism;
import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.CodePlagiarismRepository;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;

import jakarta.transaction.Transactional;

@Service
public class ScoreManager {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private CodePlagiarismRepository codePlagiarismRepository;

    // Hàm cập nhật đánh giá đạo văn và lưu trạng thái đạo văn
    @Transactional
    boolean saveScoreRecord(Source_Detail detail, String isSuspicious, List<CodePlagiarismResult> codePlagiarismResults, String otherStudentCode, double plagiarismPercentage) {
        Score score = scoreRepository.findByStudentStudentId(detail.getStudent().getStudentId());
        if (score == null) {
            score = new Score();
            score.setStudent(detail.getStudent());
        }

        if (isSuspicious != null) {
            score.setPlagiarismReason(
                    "Plagiarism percentage: " + String.format("%.2f", plagiarismPercentage)
                    + "% with students: " + otherStudentCode);
            score.setLevelOfPlagiarism(isSuspicious);
        } else if (!checkPlagiarismExist(detail.getStudent().getStudentId())) {
            score.setLevelOfPlagiarism("No plagiarism");
            score.setPlagiarismReason("N/A");
        }

        Score savedScore = scoreRepository.save(score);
        if (codePlagiarismResults != null) {
            saveCodePlagiarism(codePlagiarismResults, savedScore);
        }
        return savedScore != null && (codePlagiarismResults == null || !codePlagiarismResults.isEmpty());
    }

    private void saveCodePlagiarism(List<CodePlagiarismResult> codePlagiarismResults, Score score) {
        for (CodePlagiarismResult result : codePlagiarismResults) {
            Code_Plagiarism codePlagiarism = new Code_Plagiarism();
            codePlagiarism.setScore(score);
            codePlagiarism.setSelfCode(result.getSelfCode());
            codePlagiarism.setStudentCodePlagiarism(result.getStudentCodePlagiarism());
            codePlagiarism.setStudentPlagiarism(result.getStudentPlagiarism());
            codePlagiarismRepository.save(codePlagiarism);
        }
    }

    private boolean checkPlagiarismExist(Long studentId) {
        Score score = scoreRepository.findByStudentStudentId(studentId);
        if (score != null) {
            String plagiarismReason = score.getPlagiarismReason();
            if (plagiarismReason != null && !plagiarismReason.equals("N/A")) {
                return true;
            }
        }
        return false;
    }
}
