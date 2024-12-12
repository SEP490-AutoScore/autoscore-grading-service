package com.CodeEvalCrew.AutoScore.utils;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;

@Component
public class ProcessUtil {
    @Autowired
    private SSEController sseController;

    @Autowired
    private GradingProcessRepository gradingProcessRepository;

    public void pushGradingProcessStatus(Long examPaperId) {
        Optional<GradingProcess> optionalProcess = gradingProcessRepository
                .findByExamPaper_ExamPaperId(examPaperId);
        if (!optionalProcess.isPresent()) {
            throw new NoSuchElementException("Process not found");
        }
        GradingProcess gp = optionalProcess.get();
        sseController.pushGradingProcess(gp.getProcessId(), gp.getStatus(),
                gp.getStartDate(),examPaperId);
        gradingProcessRepository.save(gp);
    }
}
