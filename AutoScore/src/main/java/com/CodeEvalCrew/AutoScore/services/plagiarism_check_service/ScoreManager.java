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
    Score setScoreRecord(Source_Detail detail, boolean isSuspicious, List<String> plagiarizedSections, String otherStudentId) {
        Score score = scoreRepository.findByStudentStudentId(detail.getStudent().getStudentId());
        System.out.println("Set score record for student " + detail.getStudent().getStudentId());

        if (isSuspicious) {
            if (plagiarizedSections.size() > 5) { // Kiểm tra mức độ giống nhau
                score.setFlagReason("Definitely plagiarism with students " + otherStudentId + ".\nWith plagiarized sections" + plagiarizedSections);
                score.setFlag(true);
                System.out.println("Set plagiarism flag for student " + detail.getStudent().getStudentId());
            } else {
                score.setFlagReason("Possibility of plagiarism with students " + otherStudentId + ".\nWith plagiarized sections" + plagiarizedSections);
                score.setFlag(false);
                System.out.println("Set non-plagiarism flag for student " + detail.getStudent().getStudentId());
            }

        } else {
            score.setFlag(null);
            score.setFlagReason("No plagiarism");
            System.out.println("Set no flag for student " + detail.getStudent().getStudentId());
        }
        return score;
    }
}
