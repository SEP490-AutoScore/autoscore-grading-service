package com.CodeEvalCrew.AutoScore.repositories.score_repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    @Query("SELECT s FROM Student s WHERE s.studentId = :studentId")
    Student findStudentById(@Param("studentId") Long studentId);

    Score findByStudentStudentId(Long studentId);

    List<Score> findByExamPaperExamPaperId(Long examPaperId);

    @Query("SELECT s FROM Score s WHERE s.student.studentId = :studentId AND s.examPaper.examPaperId = :examPaperId")
    Score findByStudentIdAndExamPaperId(@Param("studentId") Long studentId, @Param("examPaperId") Long examPaperId);
}
