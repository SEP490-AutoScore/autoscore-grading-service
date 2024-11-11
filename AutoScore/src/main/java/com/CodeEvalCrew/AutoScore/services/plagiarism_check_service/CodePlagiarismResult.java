package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodePlagiarismResult {
    @Lob
    private String selfCode;
    private String studentCodePlagiarism;
    @Lob
    private String studentPlagiarism;
}
