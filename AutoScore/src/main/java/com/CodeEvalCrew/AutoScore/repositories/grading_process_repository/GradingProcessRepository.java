package com.CodeEvalCrew.AutoScore.repositories.grading_process_repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;

@Repository
public interface GradingProcessRepository extends JpaRepository<GradingProcess, Long>{
    // Method to find a GradingProcess by examPaperId
    Optional<GradingProcess> findByExamPaper_ExamPaperId(Long examPaperId);
}
