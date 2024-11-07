package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class CodeNormalizer {
    @SuppressWarnings("CallToPrintStackTrace")
    String normalizeCode(String folderPath) {
        StringBuilder combinedCode = new StringBuilder();

        try {
            // Duyệt qua tất cả các tệp .cs trong thư mục (bao gồm cả thư mục con)
            Stream<Path> filePaths = Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cs"));

            // Đọc và kết hợp mã từ từng tệp .cs
            for (Path path : filePaths.collect(Collectors.toList())) {
                try {
                    String code = Files.readString(path);
                    combinedCode.append(code).append("\n");
                } catch (IOException e) {
                    System.err.println("Error reading file: " + path);
                    e.printStackTrace(); // In lỗi chi tiết nhưng không dừng chương trình
                }
            }

            if (combinedCode.length() == 0) {
                System.err.println("No source code is read from the directory.");
                return "";
            }

            String code = combinedCode.toString();

            // Loại bỏ tất cả các dòng `using` và khai báo `namespace`
            code = code.replaceAll("(?m)^\\s*using\\s.*;", ""); // Loại bỏ các dòng using
            code = code.replaceAll("(?m)^\\s*namespace\\s.*\\{", ""); // Loại bỏ các dòng namespace

            // Loại bỏ phần khai báo lớp bên ngoài
            code = code.replaceAll("(?s)(public|private|protected)?\\s*class\\s+[^{]+\\{", "");

            // Chỉ lấy phần thân của các phương thức trong lớp
            Pattern methodPattern = Pattern.compile("(public|private|protected)?\\s+(static\\s+)?\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{(.*?)\\}", Pattern.DOTALL);
            Matcher matcher = methodPattern.matcher(code);
            StringBuilder extractedMethods = new StringBuilder();

            while (matcher.find()) {
                String methodBody = matcher.group(3);  // Lấy phần thân hàm
                extractedMethods.append(methodBody).append("\n");
            }

            // Chuẩn hóa lại mã đã lấy
            return extractedMethods.toString().replaceAll("\\s+", " ").trim();

        } catch (IOException e) {
            System.err.println("Error while browsing directory: " + folderPath);
            e.printStackTrace();
            return "";
        } catch (Exception e) {
            System.err.println("Unknown error during source code extraction.");
            e.printStackTrace();
            return "";
        }
    }
}
