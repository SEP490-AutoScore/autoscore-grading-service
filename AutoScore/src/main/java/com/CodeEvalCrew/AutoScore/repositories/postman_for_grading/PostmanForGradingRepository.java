package com.CodeEvalCrew.AutoScore.repositories.postman_for_grading;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.Postman_For_Grading;
@Repository
public interface PostmanForGradingRepository extends JpaRepository<Postman_For_Grading, Long>{ 
    List<Postman_For_Grading> findByExamQuestion_ExamPaper_ExamPaperId(Long examPaperId);
     // Truy vấn danh sách Postman_For_Grading theo examPaperId và sắp xếp theo orderBy
    // @Query("SELECT p FROM Postman_For_Grading p WHERE p.examPaper.examPaperId = :examPaperId ORDER BY p.orderBy")
    // List<Postman_For_Grading> findByExamPaperIdOrderByOrderBy(@Param("examPaperId") Long examPaperId);
    // @Query("SELECT p FROM Postman_For_Grading p WHERE p.examPaper.examPaperId = :examPaperId AND p.status = true ORDER BY p.orderBy")
    // List<Postman_For_Grading> findByExamPaperIdAndStatusTrueOrderByOrderBy(@Param("examPaperId") Long examPaperId);
    List<Postman_For_Grading> findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderByAsc(Long examPaperId);

}
