package com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.Important;

import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StudentSource {
    private Student student;
    private Source_Detail sourceDetail;
}
