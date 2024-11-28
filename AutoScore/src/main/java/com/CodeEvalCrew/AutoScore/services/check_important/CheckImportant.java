package com.CodeEvalCrew.AutoScore.services.check_important;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.Important.StudentSource;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.models.Entity.Important;
import com.CodeEvalCrew.AutoScore.models.Entity.Important_Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;
import com.CodeEvalCrew.AutoScore.repositories.exam_repository.IExamPaperRepository;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;
import com.CodeEvalCrew.AutoScore.repositories.important_repository.ImportantExamPaperRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
import com.CodeEvalCrew.AutoScore.repositories.student_repository.StudentRepository;
import com.CodeEvalCrew.AutoScore.utils.SourceCheckUtil;

@Service
public class CheckImportant implements ICheckImportant{
    @Autowired
    private final SourceCheckUtil utils;
    @Autowired
    private final StudentRepository studentRepository;
    @Autowired
    private final GradingProcessRepository gradingProcessRepository;
    @Autowired
    private final SourceDetailRepository sourceDetailREpository;
    @Autowired
    private final ImportantExamPaperRepository importantExamPaperRepository;
    @Autowired
    private final IExamPaperRepository examPaperRepository;

    private final SSEController sseController;
    public CheckImportant(SourceCheckUtil utils,
                            StudentRepository studentRepository,
                            GradingProcessRepository gradingProcessRepository,
                            IExamPaperRepository examPaperRepository,
                            SourceDetailRepository sourceDetailREpository,
                            SSEController sseController,
                            ImportantExamPaperRepository importantExamPaperRepository
                            ) {
                                this.studentRepository = studentRepository;
                                this.examPaperRepository = examPaperRepository;
                                this.sourceDetailREpository = sourceDetailREpository;
                                this.importantExamPaperRepository = importantExamPaperRepository;
                                this.utils = utils;
                                this.gradingProcessRepository = gradingProcessRepository;
                                this.sseController = sseController;
    }

    @Override
    public List<StudentSourceInfoDTO> checkImportantForGranding(CheckImportantRequest request) throws Exception, NotFoundException {
        List<StudentSourceInfoDTO> result;
        try {
            //check examPaper
            Exam_Paper examPaper = checkEntityExistence(examPaperRepository.findById(request.getExamPaperId()), "Exam Paper", request.getExamPaperId());

            //getListimportant
            List<Important> importants = getAllImportantByExamPaperId(request.getExamPaperId());
            if (importants.isEmpty()) {
                throw new NoSuchElementException("There are no important to check");
            }

            //List student for grading
            List<StudentSource> students = new ArrayList<>();

            //getListStudent
            for (Long studentForGrading : request.getListStudent()) {
                Student stu = checkEntityExistence(studentRepository.findById(studentForGrading), "Student", studentForGrading);
                Source_Detail source_Detail = getSourceDetailByStudentIdAndExamPaperId(studentForGrading, request.getExamPaperId());
                students.add(new StudentSource(stu, source_Detail));
            }

            result = utils.getImportantToCheck(importants, students, examPaper);
            Optional<GradingProcess> optionalProcess = gradingProcessRepository.findByExamPaper_ExamPaperId(examPaper.getExamPaperId());
            if (!optionalProcess.isPresent()) {
                throw new NoSuchElementException("process not found");
            }
            GradingProcess gp = optionalProcess.get();
            gp.setStatus(GradingStatusEnum.GRADING);
            sseController.pushGradingProcess(gp.getProcessId(), gp.getStatus(), gp.getStartDate(), request.getExamPaperId());
            gradingProcessRepository.save(gp);

            return result;
        } catch (NotFoundException | NoSuchElementException nfe) {
            throw nfe;
        } catch (Exception e) {
            System.out.println(e.getCause());
            throw e;
        }
    }

    private <T> T checkEntityExistence(Optional<T> entity, String entityName, Long entityId) throws NotFoundException {
        return entity.orElseThrow(() -> new NotFoundException(entityName + " id: " + entityId + " not found"));
    }

    private List<Important> getAllImportantByExamPaperId(Long examPaperId) {
        // Retrieve ImportantExamPaper entries and map to Important entities
        return importantExamPaperRepository.findByExamPaperExamPaperId(examPaperId)
            .stream()
            .map(Important_Exam_Paper::getImportant)
            .collect(Collectors.toList());
    }

    private Source_Detail getSourceDetailByStudentIdAndExamPaperId(Long studentId, Long examPaperId) throws NotFoundException {
        try {
            return sourceDetailREpository.findByStudentStudentIdAndSourceExamPaperExamPaperId(studentId, examPaperId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("SourceDetail not found for studentId: " + studentId + " and examPaperId: " + examPaperId);
        }
    }
}
