package com.CodeEvalCrew.AutoScore.models.DTO.ResponseDTO;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradingProcessDTO {
    private Long processId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime updateDate;
    private Long examPaperId;

    public GradingProcessDTO(Long processId, String status, LocalDateTime startDate, LocalDateTime updateDate, Long examPaperId) {
        this.processId = processId;
        this.status = status;
        this.startDate = startDate;
        this.updateDate = updateDate;
        this.examPaperId = examPaperId;
    }

    // Getters và Setters
}