package com.CodeEvalCrew.AutoScore.repositories.important_repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CodeEvalCrew.AutoScore.models.Entity.Important_Exam_Paper;

public interface ImportantExamPaperRepository extends JpaRepository<Important_Exam_Paper, Long>{
    // Derived query method to find all ImportantExamPaper entries by examPaperId
    List<Important_Exam_Paper> findByExamPaperExamPaperId(Long examPaperId);

}
