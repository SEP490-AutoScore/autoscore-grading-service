package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;

@Service
public class ScoreManager {

    @Autowired
    private ScoreRepository scoreRepository;

    // Hàm cập nhật đánh giá đạo văn và lưu trạng thái đạo văn
    Score setScoreRecord(Source_Detail detail, String isSuspicious, List<String> plagiarizedSections, String otherStudentCode, double plagiarismPercentage) {
        Score score = scoreRepository.findByStudentStudentId(detail.getStudent().getStudentId());
        if (isSuspicious != null) {
            score.setPlagiarismReason(
                    "Plagiarism percentage: " + String.format("%.2f", plagiarismPercentage)
                    + "% with students: " + otherStudentCode);
            score.setCodePlagiarism(plagiarizedSections.toString());
            score.setLevelOfPlagiarism(isSuspicious);

        } else {
            if(!checkPlagiarismExist(detail.getStudent().getStudentId())){
                score.setLevelOfPlagiarism("No plagiarism");
                score.setPlagiarismReason("N/A");
                score.setCodePlagiarism("N/A");
            }
        }
        return score;
    }

    private boolean checkPlagiarismExist(Long studentId) {
        Score score = scoreRepository.findByStudentStudentId(studentId);
        if (score != null) {
            String plagiarismReason = score.getPlagiarismReason();
            if(plagiarismReason != null && !plagiarismReason.equals("N/A")) return true;
        }
        return false;
    }
}
