package com.CodeEvalCrew.AutoScore.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.services.grading_service.IGradingService;

@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final IGradingService gradingService;

    public GradingController(
            IGradingService gradingService) {
        this.gradingService = gradingService;
    }

    @PostMapping("")
    public ResponseEntity<?> grading(@RequestBody CheckImportantRequest request) {
        try {
            gradingService.grading(request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/v2")
    public ResponseEntity<?> gradingV2(@RequestBody CheckImportantRequest request) {
        try {
            gradingService.gradingV2(request);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
