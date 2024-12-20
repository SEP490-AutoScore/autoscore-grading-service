package com.CodeEvalCrew.AutoScore.models.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Code_Plagiarism {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codePlagiarismId;
    @Lob
    private String selfCode;
    private String studentCodePlagiarism;
    @Lob
    private String studentPlagiarism;
    private String plagiarismPercentage;
    private String type;

    @ManyToOne
    @JoinColumn(name = "scoreId", nullable = false)
    private Score score;
}
