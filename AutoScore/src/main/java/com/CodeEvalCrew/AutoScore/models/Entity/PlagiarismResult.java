package com.CodeEvalCrew.AutoScore.models.Entity;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlagiarismResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long plagiarismResultId;
    private String checkType;
    private Student student;
    private Student dbStudent;
    private boolean isPlagiarized;
    private double similarity;
    private Map<String, Integer> tokenFrequency;
}
