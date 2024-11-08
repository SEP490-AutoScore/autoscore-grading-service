package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.List;

public class FingerprintRecord {
    private final String studentId;
    private final List<String> segments;

    public FingerprintRecord(String studentId, List<String> segments) {
        this.studentId = studentId;
        this.segments = segments;
    }

    public String getStudentId() {
        return studentId;
    }

    public List<String> getSegments() {
        return segments;
    }
}
