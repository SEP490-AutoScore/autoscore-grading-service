// package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;

// import org.springframework.stereotype.Service;

// import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;
// import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;

// @Service
// public class IntermediateCheck implements CheckModule {

//     @Override
//     public PlagiarismResult run(Source_Detail sourceDetail, Source_Detail dbSourceDetail) {
//         String astRepresentation = generateAST(sourceDetail.getStudentSourceCodePath());
//         String dbASTRepresentation = generateAST(dbSourceDetail.getStudentSourceCodePath());
//         double similarity = compareAST(astRepresentation, dbASTRepresentation);
//         boolean isPlagiarized = similarity >= 0.85;
        
//         return new PlagiarismResult(null, "Intermediate", sourceDetail, dbSourceDetail, isPlagiarized, similarity, null);
//     }

//     @SuppressWarnings("CallToPrintStackTrace")
//     private String generateAST(String folderPath) {
//         StringBuilder astRepresentation = new StringBuilder();
//         try {
//             // Duyệt qua tất cả các tệp .cs trong thư mục (bao gồm cả thư mục con)
//             Files.walk(Paths.get(folderPath))
//                     .filter(Files::isRegularFile)
//                     .filter(path -> path.toString().endsWith(".cs"))
//                     .forEach(path -> {
//                         try {
//                             String code = Files.readString(path);
//                             // Tạo AST từ mã nguồn
//                             String ast = parseCodeToAST(code);
//                             astRepresentation.append(ast).append("\n");
//                         } catch (IOException e) {
//                             System.err.println("Error reading file: " + path);
//                             e.printStackTrace();
//                         }
//                     });

//         } catch (IOException e) {
//             System.err.println("Error while browsing directory: " + folderPath);
//             e.printStackTrace();
//         }
//         return astRepresentation.toString();
//     }

//     private String parseCodeToAST(String code) {
//         code = code.replaceAll("\\s+", " ");  // Loại bỏ khoảng trắng dư thừa
//         code = code.replaceAll("[;{}()]", ""); // Loại bỏ dấu chấm phẩy và dấu ngoặc
//         return code;
//     }

//     private double compareAST(String ast1, String ast2) {
//         // Sử dụng thuật toán Cosine Similarity để so sánh hai AST
//         int matchingCharacters = 0;
//         int maxLength = Math.max(ast1.length(), ast2.length());

//         // Duyệt qua các ký tự trong hai biểu diễn AST để tính toán độ tương đồng
//         for (int i = 0; i < Math.min(ast1.length(), ast2.length()); i++) {
//             if (ast1.charAt(i) == ast2.charAt(i)) {
//                 matchingCharacters++;
//             }
//         }
        
//         // Tính tỷ lệ độ tương đồng
//         return (double) matchingCharacters / maxLength;
//     }
// }
