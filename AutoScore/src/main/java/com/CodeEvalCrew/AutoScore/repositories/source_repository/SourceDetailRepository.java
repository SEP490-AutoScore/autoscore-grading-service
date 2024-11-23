package com.CodeEvalCrew.AutoScore.repositories.source_repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Exam_Type_Enum;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;

@Repository
public interface SourceDetailRepository extends JpaRepository<Source_Detail, Long> {
    List<Source_Detail> findBySource_ExamPaper_ExamPaperIdOrderByStudent_StudentId(Long examPaperId);
    // Spring Data JPA will derive the query based on method name
    Source_Detail  findByStudentStudentIdAndSourceExamPaperExamPaperId(Long studentId, Long examPaperId);
    List<Source_Detail> findAllByTypeAndStudentOrganizationOrganizationId(Exam_Type_Enum examTypeEnum, Long organizationId);
}