// package com.CodeEvalCrew.AutoScore.controllers;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
// import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
// import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
// import com.CodeEvalCrew.AutoScore.services.autoscore_postman_service.IAutoscorePostmanService;
// import com.CodeEvalCrew.AutoScore.services.check_important.CheckImportant;

// import io.swagger.v3.oas.annotations.parameters.RequestBody;

// @RestController
// @RequestMapping("/api/autoscore-postman")
// public class AutoScorePostmanController {

//     @Autowired
//     private IAutoscorePostmanService autoscorePostmanService;
//     @Autowired
//     private CheckImportant checkImportant;

//     @GetMapping("")
//     public List<StudentSourceInfoDTO> gradingFunction(
//         @RequestParam Long examPaperId,
//         @RequestParam(name = "numberDeploy", required = false, defaultValue = "3") int numberDeploy
//     ) {
//         return autoscorePostmanService.gradingFunction(examPaperId, numberDeploy);
//     }

//     @PostMapping("")
//     public ResponseEntity<?> checkImportantSource(@RequestBody CheckImportantRequest request) {
//         try {
//             var result = checkImportant.checkImportantForGranding(request);
            
//             return new ResponseEntity<>(result, HttpStatus.OK);
//         } catch (NotFoundException e) {
//             return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
//         } catch (Exception e) {
//             return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//         }
//     }
// }
