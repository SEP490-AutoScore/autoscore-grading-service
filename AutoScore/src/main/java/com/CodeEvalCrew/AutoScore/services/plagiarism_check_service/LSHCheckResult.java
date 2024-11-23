package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LSHCheckResult {
    private String isSuspicious;
    private List<String> matchingSegments;
    private String otherStudentId;
    private double matchPercentage;
}
