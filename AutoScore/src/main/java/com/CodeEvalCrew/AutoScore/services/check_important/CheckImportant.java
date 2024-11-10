// package com.CodeEvalCrew.AutoScore.services.check_important;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.NoSuchElementException;
// import java.util.Optional;
// import java.util.stream.Collectors;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.dao.EmptyResultDataAccessException;
// import org.springframework.stereotype.Service;

// import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
// import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
// import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.Important.StudentSource;
// import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.StudentForGrading;
// import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
// import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
// import com.CodeEvalCrew.AutoScore.models.Entity.Important;
// import com.CodeEvalCrew.AutoScore.models.Entity.Important_Exam_Paper;
// import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
// import com.CodeEvalCrew.AutoScore.models.Entity.Student;
// import com.CodeEvalCrew.AutoScore.repositories.exam_repository.IExamPaperRepository;
// import com.CodeEvalCrew.AutoScore.repositories.important_repository.ImportantExamPaperRepository;
// import com.CodeEvalCrew.AutoScore.repositories.important_repository.ImportantRepository;
// import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
// import com.CodeEvalCrew.AutoScore.repositories.student_repository.StudentRepository;
// import com.CodeEvalCrew.AutoScore.utils.SourceCheckUtil;

// @Service
// public class CheckImportant implements ICheckImportant{
//     @Autowired
//     private final SourceCheckUtil utils;
//     @Autowired
//     private final StudentRepository studentRepository;
//     @Autowired
//     private final ImportantRepository importantRepository;
//     @Autowired
//     private final SourceDetailRepository sourceDetailREpository;
//     @Autowired
//     private final ImportantExamPaperRepository importantExamPaperRepository;
//     @Autowired
//     private final IExamPaperRepository examPaperRepository;

//     public CheckImportant(SourceCheckUtil utils,
//                             StudentRepository studentRepository,
//                             ImportantRepository importantRepository,
//                             IExamPaperRepository examPaperRepository,
//                             SourceDetailRepository sourceDetailREpository,
//                             ImportantExamPaperRepository importantExamPaperRepository
//                             ) {
//         this.importantRepository = importantRepository;
//         this.studentRepository = studentRepository;
//         this.examPaperRepository = examPaperRepository;
//         this.sourceDetailREpository = sourceDetailREpository;
//         this.importantExamPaperRepository = importantExamPaperRepository;
//         this.utils = utils;
//     }

//     @Override
//     public List<StudentSourceInfoDTO> checkImportantForGranding(CheckImportantRequest request) throws Exception, NotFoundException {
//         List<StudentSourceInfoDTO> result;
//         try {
//             //check examPaper
//             Exam_Paper examPaper = checkEntityExistence(examPaperRepository.findById(request.getExamPaperId()), "Exam Paper", request.getExamPaperId());

//             //getListimportant
//             List<Important> importants = getAllImportantByExamPaperId(request.getExamPaperId());
//             if (importants.isEmpty()) {
//                 throw new NoSuchElementException("There are no important to check");
//             }

//             //List student for grading
//             List<StudentSource> students = new ArrayList<>();

//             //getListStudent
//             for (StudentForGrading studentForGrading : request.getListStudent()) {
//                 Student stu = checkEntityExistence(studentRepository.findById(studentForGrading.getStudentId()), "Student", studentForGrading.getStudentId());
//                 Source_Detail source_Detail = getSourceDetailByStudentIdAndExamPaperId(studentForGrading.getStudentId(), request.getExamPaperId());
//                 students.add(new StudentSource(stu, source_Detail));
//             }

//             result = utils.getImportantToCheck(importants, students, examPaper);

//             return result;
//         } catch (NotFoundException | NoSuchElementException nfe) {
//             throw nfe;
//         } catch (Exception e) {
//             System.out.println(e.getCause());
//             throw e;
//         }
//     }

//     private <T> T checkEntityExistence(Optional<T> entity, String entityName, Long entityId) throws NotFoundException {
//         return entity.orElseThrow(() -> new NotFoundException(entityName + " id: " + entityId + " not found"));
//     }

//     private List<Important> getAllImportantByExamPaperId(Long examPaperId) {
//         // Retrieve ImportantExamPaper entries and map to Important entities
//         return importantExamPaperRepository.findByExamPaperExamPaperId(examPaperId)
//             .stream()
//             .map(Important_Exam_Paper::getImportant)
//             .collect(Collectors.toList());
//     }

//     private Source_Detail getSourceDetailByStudentIdAndExamPaperId(Long studentId, Long examPaperId) throws NotFoundException {
//         try {
//             return sourceDetailREpository.findByStudentStudentIdAndSourceExamPaperExamPaperId(studentId, examPaperId);
//         } catch (EmptyResultDataAccessException e) {
//             throw new NotFoundException("SourceDetail not found for studentId: " + studentId + " and examPaperId: " + examPaperId);
//         }
//     }
// }
