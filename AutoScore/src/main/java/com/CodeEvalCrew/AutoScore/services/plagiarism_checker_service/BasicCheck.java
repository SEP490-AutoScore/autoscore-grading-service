// package com.CodeEvalCrew.AutoScore.services.plagiarism_checker_service;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;

// import org.jvnet.hk2.annotations.Service;

// import com.CodeEvalCrew.AutoScore.models.Entity.PlagiarismResult;
// import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;

// @Service
// public class BasicCheck implements CheckModule {

//     @Override
//     public PlagiarismResult run(Source_Detail sourceDetail, Source_Detail dbSourceDetail) {
//         String normalizedCode = normalizeCode(sourceDetail.getStudentSourceCodePath());
//         String dbNormalizedCode = normalizeCode(dbSourceDetail.getStudentSourceCodePath());
//         double similarity = calculateStringSimilarity(normalizedCode, dbNormalizedCode);
//         Map<String, Integer> tokenFrequency = analyzeTokenFrequency(normalizedCode);

//         return new PlagiarismResult(null, "Basic", sourceDetail, dbSourceDetail, similarity >= 0.7, similarity, tokenFrequency);
//     }

//     @SuppressWarnings("CallToPrintStackTrace")
//     private String normalizeCode(String folderPath) {
//         StringBuilder combinedCode = new StringBuilder();
//         try {
//             // Duyệt qua tất cả các tệp .cs trong thư mục (bao gồm cả thư mục con)
//             Stream<Path> filePaths = Files.walk(Paths.get(folderPath))
//                     .filter(Files::isRegularFile)
//                     .filter(path -> path.toString().endsWith(".cs"));

//             // Đọc và kết hợp mã từ từng tệp .cs
//             for (Path path : filePaths.collect(Collectors.toList())) {
//                 try {
//                     String code = Files.readString(path);
//                     combinedCode.append(code).append("\n");
//                 } catch (IOException e) {
//                     System.err.println("Error reading file: " + path);
//                     e.printStackTrace(); // In lỗi chi tiết nhưng không dừng chương trình
//                 }
//             }

//             if (combinedCode.length() == 0) {
//                 System.err.println("No source code is read from the directory.");
//                 return "";
//             }

//             String code = combinedCode.toString();

//             // Loại bỏ các dòng `using`, `namespace`, và các dòng chú thích
//             code = code.replaceAll("(?m)^\\s*using\\s.*;", ""); // Loại bỏ các dòng using
//             code = code.replaceAll("(?m)^\\s*namespace\\s.*\\{", ""); // Loại bỏ các dòng namespace
//             code = code.replaceAll("//.*", ""); // Loại bỏ chú thích đơn dòng
//             code = code.replaceAll("/\\*.*?\\*/", ""); // Loại bỏ chú thích đa dòng

//             // Loại bỏ phần khai báo lớp bên ngoài
//             code = code.replaceAll("(?s)(public|private|protected)?\\s*class\\s+[^{]+\\{", "");

//             // Chỉ lấy phần thân của các phương thức trong lớp
//             Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s+(static\\s+)?\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{(.*?)\\}", Pattern.DOTALL);
//             Matcher matcher = methodPattern.matcher(code);
//             StringBuilder extractedMethods = new StringBuilder();

//             while (matcher.find()) {
//                 String methodBody = matcher.group(3);  // Lấy phần thân hàm
//                 extractedMethods.append(methodBody).append("\n");
//             }

//             // Chuẩn hóa lại mã đã lấy
//             return extractedMethods.toString().replaceAll("\\s+", " ").trim();

//         } catch (IOException e) {
//             System.err.println("Error while browsing directory: " + folderPath);
//             e.printStackTrace();
//             return "";
//         } catch (Exception e) {
//             System.err.println("Unknown error during source code extraction.");
//             e.printStackTrace();
//             return "";
//         }
//     }

//     private double calculateStringSimilarity(String code1, String code2) {
//         Map<String, Integer> vector1 = createTermFrequencyMap(code1);
//         Map<String, Integer> vector2 = createTermFrequencyMap(code2);

//         double dotProduct = 0.0;
//         double normA = 0.0;
//         double normB = 0.0;

//         for (String key : vector1.keySet()) {
//             int frequencyA = vector1.getOrDefault(key, 0);
//             int frequencyB = vector2.getOrDefault(key, 0);

//             dotProduct += frequencyA * frequencyB;
//             normA += Math.pow(frequencyA, 2);
//         }

//         for (int frequencyB : vector2.values()) {
//             normB += Math.pow(frequencyB, 2);
//         }

//         // Kiểm tra trường hợp normA hoặc normB bằng 0 để tránh chia cho 0
//         if (normA == 0 || normB == 0) return 0.0;

//         return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
//     }

//     private Map<String, Integer> createTermFrequencyMap(String code) {
//         Map<String, Integer> termFrequency = new HashMap<>();
//         String[] tokens = code.split("\\W+");

//         for (String token : tokens) {
//             if (!token.isEmpty()) {
//                 termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
//             }
//         }
//         return termFrequency;
//     }

//     private Map<String, Integer> analyzeTokenFrequency(String code) {
//         Map<String, Integer> tokenFreq = new HashMap<>();
//         String[] tokens = code.split("\\W+");

//         for (String token : tokens) {
//             if (!token.isEmpty()) {
//                 tokenFreq.put(token, tokenFreq.getOrDefault(token, 0) + 1);
//             }
//         }
//         return tokenFreq;
//     }
// }