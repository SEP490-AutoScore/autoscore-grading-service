package com.CodeEvalCrew.AutoScore.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.CheckImportantRequest;
import com.CodeEvalCrew.AutoScore.services.check_important.ICheckImportant;


@RestController
@RequestMapping("/api/autoscore-check-important")
public class ImportantCheckController {
    @Autowired
    private ICheckImportant checkImportant;

    @PostMapping("")
    public ResponseEntity<?> checkImportant(@RequestBody CheckImportantRequest request) {
        try {
            var result = checkImportant.checkImportantForGranding(request);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch(NotFoundException e){
            return new ResponseEntity<>(e.getMessage() ,HttpStatus.INTERNAL_SERVER_ERROR);
        } catch(Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
 }
