package com.CodeEvalCrew.AutoScore.services.score_service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;
import com.CodeEvalCrew.AutoScore.models.Entity.Student_Error;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;
import com.CodeEvalCrew.AutoScore.repositories.student_repository.StudentErrorRepository;

@Service
public class ScoreService implements IScoreService{
    private final ScoreRepository scoreRepository;
    private final StudentErrorRepository studentErrorRepository;

    public ScoreService(StudentErrorRepository studentErrorRepository, ScoreRepository scoreRepository) {
        this.studentErrorRepository = studentErrorRepository;
        this.scoreRepository = scoreRepository;
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void addStudentErrorToScore(Long examPaperId) {
        try {
            List<Student_Error> studentErrors = studentErrorRepository.findBySourceExamPaperExamPaperId(examPaperId);
            for (Student_Error studentError : studentErrors) {
                saveScore(studentError.getStudent(), studentError.getSource().getExamPaper());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private Score saveScore(Student student, Exam_Paper examPaper){
        try {
            System.out.println("Saving student error score for student: " + student.getStudentCode());
            Score score = new Score();
            score.setStudent(student);
            score.setExamPaper(examPaper);
            score.setTotalScore(0.0f);
            score.setGradedAt(LocalDateTime.now());
            score.setReason("Student source code has error (solution not found, zipped file can't be unzipped, zipped file is corrupted, ...).");
            scoreRepository.save(score);
            return score;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
