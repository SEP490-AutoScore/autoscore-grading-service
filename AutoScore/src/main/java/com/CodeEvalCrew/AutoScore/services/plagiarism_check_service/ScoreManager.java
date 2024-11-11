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
    boolean saveScoreRecord(Source_Detail detail, String isSuspicious, List<CodePlagiarismResult> codePlagiarismResults, double plagiarismPercentage) {
        Score score = scoreRepository.findByStudentStudentId(detail.getStudent().getStudentId());
        if (score == null) {
            score = new Score();
            score.setStudent(detail.getStudent());
        }

        if (isSuspicious != null) {
            // Dùng StringBuilder để lưu tất cả sinh viên bị nghi ngờ có mã đạo văn
            // StringBuilder studentCodePlagiarism = new StringBuilder();
            // if (score.getCodePlagiarisms() != null) {
            //     for (Code_Plagiarism codePlagiarism : score.getCodePlagiarisms()) {
            //         if (codePlagiarism.getStudentCodePlagiarism() != null) {
            //             studentCodePlagiarism.append(codePlagiarism.getStudentCodePlagiarism()).append(", ");
            //         }
            //     }
            // }

            // Thêm tất cả sinh viên từ codePlagiarismResults
            // for (CodePlagiarismResult result : codePlagiarismResults) {
            //     String plagiarizedStudentCode = result.getStudentCodePlagiarism();
            //     if (plagiarizedStudentCode != null && !studentCodePlagiarism.toString().contains(plagiarizedStudentCode)) {
            //         studentCodePlagiarism.append(plagiarizedStudentCode).append(", ");
            //     }
            // }

            // // Loại bỏ dấu phẩy cuối cùng và thêm dấu chấm
            // if (studentCodePlagiarism.length() > 0) {
            //     studentCodePlagiarism.setLength(studentCodePlagiarism.length() - 2); // Xóa dấu phẩy cuối
            //     studentCodePlagiarism.append("."); // Thêm dấu chấm
            // }

            // String plagiarismWithStudents = studentCodePlagiarism.toString();

            int plagiarismWithStudents = codePlagiarismResults.size();

            // Thiết lập lý do đạo văn và mức độ đạo văn
            score.setPlagiarismReason("Plagiarism percentage: " + String.format("%.2f", plagiarismPercentage)
                    + "% with " + plagiarismWithStudents + " students." );
            score.setLevelOfPlagiarism(isSuspicious);
        } else if (!checkPlagiarismExist(detail.getStudent().getStudentId())) {
            score.setLevelOfPlagiarism("No plagiarism");
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
