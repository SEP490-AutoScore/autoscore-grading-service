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
    boolean saveScoreRecord(Source_Detail detail, String isSuspicious, List<CodePlagiarismResult> codePlagiarismResults) {
        Score score = scoreRepository.findByStudentStudentId(detail.getStudent().getStudentId());
        if (score == null) {
            System.out.println("Saving student error score for student: " + detail.getStudent().getStudentCode());
            return false;
        }

        if (isSuspicious != null) {
            // Thiết lập lý do đạo văn và mức độ đạo văn
            score.setPlagiarismReason("Same with " + codePlagiarismResults.size() + " students.");
            score.setLevelOfPlagiarism(isSuspicious);
        } else if (!checkPlagiarismExist(detail.getStudent().getStudentId())) {
            score.setLevelOfPlagiarism("NORMAL");
            score.setPlagiarismReason("N/A");
        }

        Score savedScore = scoreRepository.save(score);
        if (codePlagiarismResults != null && !codePlagiarismResults.isEmpty()) {
            saveCodePlagiarism(codePlagiarismResults, savedScore);
        }
        return savedScore != null && (codePlagiarismResults == null || !codePlagiarismResults.isEmpty());
    }

    private void saveCodePlagiarism(List<CodePlagiarismResult> codePlagiarismResults, Score score) {
        for (CodePlagiarismResult result : codePlagiarismResults) {
            if (result.getSelfCode() == null || result.getSelfCode().isEmpty()
                    || result.getStudentPlagiarism() == null || result.getStudentPlagiarism().isEmpty()) {
                continue;
            }
            Code_Plagiarism codePlagiarism = new Code_Plagiarism();
            codePlagiarism.setScore(score);
            codePlagiarism.setSelfCode(result.getSelfCode());
            codePlagiarism.setStudentCodePlagiarism(result.getStudentCodePlagiarism());
            codePlagiarism.setStudentPlagiarism(result.getStudentPlagiarism());
            codePlagiarism.setPlagiarismPercentage(String.format("%.2f", result.getPlagiarismPercentage()) + "%");
            codePlagiarism.setType(result.getType());
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
