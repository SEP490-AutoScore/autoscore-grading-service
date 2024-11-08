// package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

// import org.springframework.stereotype.Service;

// import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;
// import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
// @Service
// public class AdvancedCheck implements CheckModule {

//     @Override
//     public PlagiarismResult run(Source_Detail sourceDetail, Source_Detail dbSourceDetail) {
//         String detailedAST = analyzeDetailedAST(sourceDetail.getStudentSourceCodePath());
//         String dbDetailedAST = analyzeDetailedAST(dbSourceDetail.getStudentSourceCodePath());
//         double similarity = compareDetailedAST(detailedAST, dbDetailedAST);
//         boolean isPlagiarized = similarity >= 0.95;

//         return new PlagiarismResult(null,"Advanced", sourceDetail, dbSourceDetail, isPlagiarized, similarity, null);
//     }

//     private String analyzeDetailedAST(String folderPath) {
//         // Phân tích chi tiết cây cú pháp (AST)
//         return "DETAILED_AST_REPRESENTATION";
//     }

//     private double compareDetailedAST(String detailedAST1, String detailedAST2) {
//         // So sánh AST chi tiết
//         return 0.96; // Ví dụ độ tương đồng
//     }
// }