// package com.CodeEvalCrew.AutoScore.controllers;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
// import com.CodeEvalCrew.AutoScore.services.autoscore_postman_service.IAutoscorePostmanService;

// @RestController
// @RequestMapping("/api/autoscore-postman")
// public class AutoScorePostmanController {

//     @Autowired
//     private IAutoscorePostmanService autoscorePostmanService;

   
//     @GetMapping("")
//     public List<StudentSourceInfoDTO> gradingFunction(
//         @RequestParam Long examPaperId,
//         @RequestParam(name = "numberDeploy", required = false, defaultValue = "2") int numberDeploy
//     ) {
//         return autoscorePostmanService.gradingFunction(examPaperId, numberDeploy);
//     }
// }
