package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.MalformedInputException;
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
                String code = readFileWithFallbackEncoding(path); // Đọc tệp với mã hóa dự phòng
                if (!code.isEmpty()) {
                    combinedCode.append(code).append("\n");
                }
            }

            if (combinedCode.length() == 0) {
                System.err.println("No source code is read from the directory.");
                return "";
            }

            String code = combinedCode.toString();

            // Loại bỏ các dòng `using`, `namespace`, và các dòng chú thích
            code = code.replaceAll("(?m)^\\s*using\\s.*;", ""); // Loại bỏ các dòng using
            code = code.replaceAll("(?m)^\\s*namespace\\s.*\\{", ""); // Loại bỏ các dòng namespace
            code = code.replaceAll("//.*", ""); // Loại bỏ chú thích đơn dòng
            code = code.replaceAll("/\\*.*?\\*/", ""); // Loại bỏ chú thích đa dòng

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

    // Hàm đọc tệp với mã hóa UTF-8 và thử mã hóa khác nếu gặp lỗi
    @SuppressWarnings("CallToPrintStackTrace")
    private String readFileWithFallbackEncoding(Path path) {
        try {
            // Thử đọc tệp với mã hóa UTF-8
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            System.err.println("UTF-8 decoding failed for file: " + path + ", attempting ISO-8859-1");
            try {
                // Nếu UTF-8 thất bại, thử đọc bằng ISO-8859-1
                return Files.readString(path, Charset.forName("ISO-8859-1"));
            } catch (IOException ex) {
                System.err.println("Error reading file with ISO-8859-1 encoding: " + path);
                ex.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + path);
            e.printStackTrace();
        }

        // Nếu không thể đọc tệp, trả về chuỗi rỗng
        return "";
    }
}