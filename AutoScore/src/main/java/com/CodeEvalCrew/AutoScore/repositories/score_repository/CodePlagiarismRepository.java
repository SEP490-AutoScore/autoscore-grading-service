package com.CodeEvalCrew.AutoScore.repositories.score_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CodeEvalCrew.AutoScore.models.Entity.Code_Plagiarism;

@Repository
public interface CodePlagiarismRepository extends JpaRepository<Code_Plagiarism, Long> {
    
}
