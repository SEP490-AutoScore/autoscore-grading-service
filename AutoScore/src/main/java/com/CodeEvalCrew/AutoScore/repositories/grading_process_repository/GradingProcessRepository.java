package com.CodeEvalCrew.AutoScore.repositories.grading_process_repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;

@Repository
public interface GradingProcessRepository extends JpaRepository<GradingProcess, Long>{
    // Method to find a GradingProcess by examPaperId
    Optional<GradingProcess> findByExamPaper_ExamPaperId(Long examPaperId);
    // Method to find if any GradingProcess has a status in the specified list
    boolean existsByStatusIn(List<GradingStatusEnum> statuses);
    // Method to find all GradingProcess with PENDING status
    List<GradingProcess> findByStatus(GradingStatusEnum status);
    // Method to find the first GradingProcess with PENDING status
    GradingProcess findFirstByStatus(GradingStatusEnum status); 
}
