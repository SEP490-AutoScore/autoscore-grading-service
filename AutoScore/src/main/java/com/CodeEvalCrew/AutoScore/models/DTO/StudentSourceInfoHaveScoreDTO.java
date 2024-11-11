package com.CodeEvalCrew.AutoScore.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentSourceInfoHaveScoreDTO {
    private Long sourceDetailId; 
    private Long studentId;
    private String studentSourceCodePath;
}
