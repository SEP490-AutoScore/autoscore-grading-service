package com.CodeEvalCrew.AutoScore.repositories.postman_for_grading;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.Postman_For_Grading;

@Repository
public interface PostmanForGradingRepository extends JpaRepository<Postman_For_Grading, Long> {
    List<Postman_For_Grading> findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderPriorityAsc(Long examPaperId);
}
